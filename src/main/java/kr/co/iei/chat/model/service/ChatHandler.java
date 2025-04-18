package kr.co.iei.chat.model.service;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.iei.chat.model.dto.ChatContentDTO;
import kr.co.iei.chat.model.dto.ChatRoomDTO;
import kr.co.iei.chat.model.dto.MessageDTO;
import kr.co.iei.member.model.service.MemberService;


@Component
public class ChatHandler extends TextWebSocketHandler {
	@Autowired
	@Lazy
	private MemberService memberService;
	@Autowired
	private ChatService chatService;
	private ObjectMapper om;
	// 로그인한 사람이 있는 채팅방 관리
	private HashMap<Integer, Set<String>> chatRooms;
	// List를 순회하여  loginMembers 에 있으면 해당 소켓으로 데이터 보내줌
	private HashMap<String, WebSocketSession> loginMembers;
	public ChatHandler() {
		super();
		loginMembers = new HashMap<>();
		chatRooms = new HashMap<>();
		om = new ObjectMapper();
	}
	
	// url 에서 닉네임 추출해오는 메소드
	private String getMemberNickname(String query) {
	    if (query == null || !query.startsWith("memberNickname=")) return null;
	    String encoded = query.substring("memberNickname=".length());
	    return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
	}
	// 클라이언트에 메세지 전송함수
	private void sendMessage(Object obj,WebSocketSession session) throws IOException {
		String data =om.writeValueAsString(obj) ;
		TextMessage sendData = new TextMessage(data);
		session.sendMessage(sendData);
	}
	// 채팅 그룹 최신화 메소드
	private void refreshGroup(int chatNo) throws Exception {
		// 1. 그룹 멤버셋 DB에서 다시 조회 후 저장
	    Set<String> groupSet = chatService.selectGroupSet(chatNo);
	    chatRooms.put(chatNo, groupSet);

	    // 2. 모든 로그인 유저에게 방 목록 새로 보내기
	    for (String nick : loginMembers.keySet()) {
	        WebSocketSession session = loginMembers.get(nick);
	        if (session != null && session.isOpen()) {
	            handleFetchRoomList(session);
	        }
	    }
	}
	
	// 클라이언트가 소켓에 최초 접속하면 자동으로 실행되는 메소드
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		// 로그인한 사람 그룹 로드
		URI uri = session.getUri();
		String memberNickname = getMemberNickname(uri.getQuery());
		// 로그인한 세션 저장
		loginMembers.put(memberNickname,session);
		//해당 닉넴이으로 있는 채팅방 조회
		ArrayList<Integer> roomList = (ArrayList<Integer>)chatService.selectRoomList(memberNickname);
		// 채팅방 조회후 있으면 chatRooms 에 추가
		if(!roomList.isEmpty()) {
			for(int chatNo : roomList) {
				//채팅방에 속한 사람들 조회
				refreshGroup(chatNo);
			}
		}
		
	}
	// 클라이언트가 소켓으로 데이터를 전송하면 실행되는 메소드
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		MessageDTO chat = om.readValue(message.getPayload(), MessageDTO.class);
	    String type = chat.getType();
	    int chatNo = chat.getChatNo();
	    switch (type) {
	        case "FETCH_ROOM_LIST":
	            handleFetchRoomList(session);
	            break;
	        case "SELECT_ROOM":
	        	handleSelectRoom(session, chat);
	        	break;
	        case "SEND_MESSAGE":
	            handleSendMessage(session, chat);
	            break;
	        case "CREATE_ROOM":
	            handleCreateRoom(session);
	            break;
	        case "INVITE_ROOM":
	            handleInviteRoom(session, chat);
	            break;
	        case "LEAVE_ROOM":
	        	handleLeaveRoom(session, chat);
	        	break;
	        case "UPDATE_TITLE":
	        	handleUpdateTitle(session, chat);
	        	break;
	        case "UPDATE_STATUS":
	        	handleReadStatus(session,chat);
	        	break;
	        default:
	            session.sendMessage(new TextMessage("알 수 없는 요청 타입: " + type));
	    }
	}


	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		URI uri = session.getUri();
	    String memberNickname = getMemberNickname(uri.getQuery());
	    // 세션 제거
	    loginMembers.remove(memberNickname);
	}
	//채팅방 초대
	private void handleInviteRoom(WebSocketSession session, MessageDTO chat) throws Exception {
		//1.닉네임 조회
		String nickname = chat.getMessage();
		int result = memberService.exists(nickname);
		if(result==0) {
			Map<String, Object> map = new HashMap<>();
			map.put("type", "NOT_EXIST");
		    sendMessage(map, session);
			return;
		}
		//2. 초대작업
		ChatContentDTO cc = new ChatContentDTO(chat.getChatNo(), nickname, null, null);
		result += chatService.inviteRoom(cc);
		if(result==1) {
			Map<String, Object> map = new HashMap<>();
			map.put("type", "ERROR");
		    sendMessage(map, session);
			return;
		}
		refreshGroup(cc.getChatNo());
		handleSelectRoom(session,chat);
	
	}
	//채팅방떠나기
	private void handleLeaveRoom(WebSocketSession session, MessageDTO chat) throws Exception {
		URI uri = session.getUri();
	    String memberNickname = getMemberNickname(uri.getQuery());
	    ChatContentDTO cc = new ChatContentDTO(chat.getChatNo(), memberNickname, null, null);
	    int result = chatService.leaveRoom(cc);
	    if(result>0) {
	    	refreshGroup(cc.getChatNo());
	    	handleFetchRoomList(session);
	    }
		
	}
	//채팅방 생성
	private void handleCreateRoom(WebSocketSession session) throws Exception {
		URI uri = session.getUri();
	    String memberNickname = getMemberNickname(uri.getQuery());
	    ChatContentDTO cc= new ChatContentDTO();
	    cc.setMemberNickname(memberNickname);
	    int result = chatService.createRoom(cc);
	    if(result>0) {
	    	int chatNo = cc.getChatNo();
	    	refreshGroup(chatNo);
	    }
	}
	//채팅 읽은 상태 수정
	private void handleReadStatus(WebSocketSession session, MessageDTO chat) throws Exception {
	    URI uri = session.getUri();
	    String memberNickname = getMemberNickname(uri.getQuery());
	    int chatNo = chat.getChatNo();
	    ChatRoomDTO latestInfo = chatService.selectLatestChatInfo(chatNo);
		// 최신 메시지가 없으면 → chat_read_status 업데이트 안 함
		if (latestInfo != null && latestInfo.getLatestContentNo() > 0) {
			latestInfo.setMemberNickname(memberNickname);
			chatService.updateReadStatus(latestInfo);
		}
	}
	//session 으로 방정보만 전송
	private void handleSelectRoom(WebSocketSession session, MessageDTO chat) throws Exception {
	    // 1. 읽음 처리
	    handleReadStatus(session, chat);
	    // 2. 채팅 내용 전송
	    sendChatContentOnly(session, chat);
	    // 3. 채팅방 리스트 갱신
	    handleFetchRoomList(session);
	}
	// 메시지만 보내는 메서드
	private void sendChatContentOnly(WebSocketSession session, MessageDTO chat) throws Exception {
	    int chatNo = chat.getChatNo();
	    List<ChatContentDTO> chatContent = chatService.selectChatContent(chatNo);
	    Map<String, Object> map = new HashMap<>();
	    map.put("type", "CHAT_CONTENT");
	    map.put("content", chatContent);
	    sendMessage(map, session);
	}
	//메시지 전송받았을 때 작업
	private void handleSendMessage(WebSocketSession session, MessageDTO chat) throws Exception {
	    URI uri = session.getUri();
	    String senderNickname = getMemberNickname(uri.getQuery());
	    int chatNo = chat.getChatNo();

	    ChatContentDTO cc = new ChatContentDTO(chatNo, senderNickname, null, chat.getMessage());
	    int result = chatService.insertText(cc);

	    if (result > 0) {
	    	//당사자는 읽음 처리
	    	handleReadStatus(session, chat);
	        Set<String> nickSet = chatRooms.get(chatNo);
	        for (String nick : loginMembers.keySet()) {
	            WebSocketSession target = loginMembers.get(nick);
	            if (target != null && target.isOpen()) {
	                // 메시지는 모두에게 전송
	                sendChatContentOnly(target, chat);

	                // 목록 갱신 (읽음 처리 없음)
	                handleFetchRoomList(target);
	            }
	        }
	    }
	}
	//방목록 최신화
	private void handleFetchRoomList(WebSocketSession session) throws Exception {
	    URI uri = session.getUri();
	    String memberNickname = getMemberNickname(uri.getQuery());
	    List<ChatRoomDTO> roomDataList = chatService.selectRoomData(memberNickname);
	    Map<String, Object> response = new HashMap<>();
	    response.put("type", "ROOM_LIST");
	    response.put("room", roomDataList);
	    sendMessage(response, session);
	}

	//채팅방 제목 수정 메소드
	private void handleUpdateTitle(WebSocketSession session, MessageDTO chat) throws Exception {
		int chatNo = chat.getChatNo();
		ChatRoomDTO crd = new ChatRoomDTO(chatNo, chat.getMessage(),null, null,0,0,null);
		int result = chatService.updateTitle(crd);
		if(result>0) {
			refreshGroup(chatNo);		
		}
	}

}
