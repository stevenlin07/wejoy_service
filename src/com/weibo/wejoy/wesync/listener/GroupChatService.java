package com.weibo.wejoy.wesync.listener;

import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.weibo.wejoy.service.WejoyService;
import com.weibo.wejoy.utils.SwitchController;
import com.weibo.wejoy.wesync.listener.model.GroupOperation;
import com.weibo.wejoy.wesync.listener.model.GroupOperationType;
import com.weibo.wesync.Command;
import com.weibo.wesync.CommandListener;
import com.weibo.wesync.DataService;
import com.weibo.wesync.WeSyncService;
import com.weibo.wesync.WeSyncURI;
import com.weibo.wesync.data.FolderChild;
import com.weibo.wesync.data.FolderID;
import com.weibo.wesync.data.MetaMessageType;
import com.weibo.wesync.data.WeSyncMessage.Meta;
import com.weibo.wesync.data.WeSyncMessage.SyncReq;

public class GroupChatService implements CommandListener {
	private final Logger log = LoggerFactory.getLogger(GroupChatService.class);
	
	public GroupChatService(WeSyncService weSync){
		this.weSync = weSync;
		this.weSync.registerCommandListener(this);
		this.dataService = weSync.getDataService();
	}
	
	public String createGroup(String operator, List<String> members) {
		return dataService.createGroup(operator, members);
	}
	
	/**获取群成员*/
	public HashSet<String> getGroupMembers(String gid) {
		HashSet<String> memberUids = new HashSet<String>();
		SortedSet<FolderChild> folderChildSet = dataService.members(gid);
		for(FolderChild folderChild :folderChildSet ) {
			memberUids.add(folderChild.id);
		}
		return memberUids;
	}
	
	public boolean handleGroupOperation( GroupOperation oper){
		boolean result = false;
		GroupOperationType operType = oper.getType();
		switch( operType ){
			case addMember:
				result = dataService.addMember(oper.getGid(), oper.getAffectedUsers());
				break;
			case removeMember:
				result = dataService.removeMember(oper.getGid(),  oper.getAffectedUsers());
				break;
			case quitGroup:
				result = dataService.removeMember(oper.getGid(), oper.getOperator());
				break;
			default:
				log.error("Undefined group operation from user: "+ oper.getOperator());
				throw new RuntimeException();
		}
		// Group operation notice shoule be composed and sent by upper app logic
		//dataService.broadcastMemberChange(oper.getGroupId(), operType, extendToBroadcast);
		return result;
	}
	
	@Override
	public boolean handle(Command comm, ByteString syncReqString) {
		if (comm.equals(Command.Sync)) {
			try {
				SyncReq data = SyncReq.parseFrom(syncReqString);
				if (log.isDebugEnabled()) {
					log.debug("Heard about SYNC command: " + data);
				}

				return handleSync(data);
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	private boolean handleSync(SyncReq req) {
		FolderID folderId = new FolderID( req.getFolderId() );
		
		if( folderId.type.equals( FolderID.Type.Group ) ){
			return handleSyncOnGroup(folderId, req);
		}else{
			return handleNormalSync(folderId, req);
		}
	}
	
	private boolean handleNormalSync(FolderID folderId, SyncReq req) {
		String fromUser = FolderID.getUsername(folderId);
		String userChatWith = FolderID.getUserChatWith(folderId);
		String receiverFolderId = FolderID.onConversation(userChatWith, fromUser);
		
		route(req, userChatWith, receiverFolderId);
		return true;
	}
	
	private boolean handleSyncOnGroup(FolderID folderId, SyncReq req) {
		Meta msg = null;
		if( req.getClientChangesCount() > 0 ){
			//Only support one client change per Sync
			msg = req.getClientChanges(0);
		}
		if( null == msg ) return false;
		
		String fromUser = FolderID.getUsername(folderId);
		String groupId = FolderID.getGroup(folderId);
		
		//TODO  wesync also store this, should move to servicelistener to complete storage
		//dataService.storeToGroup(msg, groupId);
		
		dataService.broadcastNewMessage(groupId, msg.getFrom(), msg.getId());

		SortedSet<FolderChild> members = dataService.members(groupId);
		for (FolderChild fc : members) {
			if( fc.id.equals(fromUser) ) continue;
			
			String recvFolderId = FolderID.onGroup(fc.id, groupId);
			route(req, fc.id, recvFolderId);			
		}
		
		if (SwitchController.isExtraRouteTaskEnabled.get()) {
			executor.submit(new ExtraRouteTask(fromUser, groupId, msg));
		}
		return true;
	}
	
	private boolean route(SyncReq req, String toUser, String toFolderId){
		SyncReq toReceiver = SyncReq.newBuilder(req)
				.setFolderId(toFolderId)
				.build();
		
		WeSyncURI uri = new WeSyncURI();
		uri.command = Command.Sync.toByte();
		uri.protocolVersion = weSync.version();
		
		weSync.handle(toUser, uri, toReceiver.toByteArray(), true);
		return true;
	}
	
	public WeSyncService getWeSync() {
		return this.weSync;
	}
	
	private class ExtraRouteTask implements Runnable {
		
		public ExtraRouteTask(String fromuid, String touid, Meta msg) {
			this.fromuid = fromuid;
			this.touid = touid;
			this.msg = msg;
		}

		@Override
		public void run() {
			MetaMessageType type = MetaMessageType.valueOf(msg.getType().byteAt(0));
			switch (type) {
				case text:
					WejoyService.sendCommentToWeibo(touid, msg.getContent().toStringUtf8(), fromuid);
					break;
				case audio:
					// TODO: aggregate and upload audio
					break;
				case file:
					// TODO: upload picture
					break;
				default:
					log.warn("unknown message type for extra route task");
			}
		}			
		String fromuid;
		String touid;
		Meta msg;
	}
	
	private WeSyncService weSync;
	private DataService dataService;
	private ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), 
			Runtime.getRuntime().availableProcessors() * 2, 60, TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<Runnable>());
}