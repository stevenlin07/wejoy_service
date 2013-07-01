package com.weibo.wejoy.wesync;

import java.io.IOException;

import com.google.protobuf.ByteString;
import com.weibo.wejoy.wesync.listener.GroupChatService;
import com.weibo.wejoy.wesync.listener.model.GroupOperation;
import com.weibo.wejoy.wesync.listener.model.GroupOperationType;
import com.weibo.wesync.Command;
import com.weibo.wesync.WeSyncURI;
import com.weibo.wesync.data.MetaMessageType;
import com.weibo.wesync.data.WeSyncMessage.Meta;
import com.weibo.wesync.data.WeSyncMessage.SyncReq;

public class SyncService {

	protected GroupChatService wejoyListener ;
	public String TAG_SYNC_KEY = "0";
	
	private String getTag (String folderId) {
		return folderId+"-"+System.currentTimeMillis();
	}
	
	private WeSyncURI getWeSyncURI(){
		WeSyncURI uri = new WeSyncURI();
		uri.protocolVersion = 20;
		uri.guid = "1234567890abcdefg";
		uri.deviceType = "iphone";		
		return uri;
	}
	
	protected boolean sendMeta(String fromuid, String touid, String content,ConvType convType) {
		String folderId = onFolderId(fromuid, touid, convType);
		String withtag = getTag(folderId);
		Meta meta = buildTextMeta(withtag, fromuid, touid, content);
		byte[] syncRequest = buildSendSync(folderId, meta).toByteArray();
		WeSyncURI uri = getWeSyncURI();
		uri.command = Command.Sync.toByte();
		try {
			wejoyListener.getWeSync().request(fromuid, WeSyncURI.toBytes(uri), syncRequest);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	protected boolean sendGroupOper(GroupOperationType type, String gid, String operator,String affectedUsers) {
		GroupOperation groupOperation = new GroupOperation(GroupOperationType.addMember,gid,operator,affectedUsers);
		return wejoyListener.handleGroupOperation(groupOperation);
	}
	
	/** Sync的请求消息体 */
	private ByteString buildSendSync(String folderId,Meta meta){
		SyncReq.Builder syncreqBuilder = SyncReq.newBuilder()
				.setFolderId(folderId).setIsFullSync(false)
				.setKey(TAG_SYNC_KEY);
		if(null != meta){
			syncreqBuilder.setIsSendOnly(true).addClientChanges(meta);
		}
		return syncreqBuilder.build().toByteString();
	}
	
	private Meta.Builder buildMeta(String withtag,String fromuid,String touid,MetaMessageType MetaMessageType){
		Meta.Builder metaBuilder = Meta
				.newBuilder()
				.setFrom(fromuid)
				.setId(withtag)
				.setTo(touid)
				.setTime((int)(System.currentTimeMillis()/1000))
				.setType(ByteString.copyFrom(new byte[] { (MetaMessageType.toByte()) }));
		return metaBuilder;
	}

	/** TextMeta */
	private Meta buildTextMeta(String withtag,String fromuid,String touid,String text){
		Meta.Builder metaBuilder = buildMeta(withtag,fromuid,touid,MetaMessageType.text);
		metaBuilder.setContent(ByteString.copyFromUtf8(text));
		return metaBuilder.build();
	}
	
	private String onFolderId(String fromuid,String touid, ConvType convType){
		String folderId = null;
		if (ConvType.SINGLE == convType) { // 单人聊天
			folderId = IdUtils.onConversation(fromuid,touid);
		} else if (ConvType.GROUP == convType) { // 群组聊天
			folderId = IdUtils.onGroup(fromuid, touid);
		}
		return folderId;
	}
}