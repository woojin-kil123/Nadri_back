package kr.co.iei.member.model.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.web.multipart.MultipartFile;

import kr.co.iei.member.model.dto.MemberDTO;

@Mapper
public interface MemberDao {
	
	int exists(String memberNickname);

	int insertMember(MemberDTO member);

	int socialJoin(MemberDTO member);
	
	int existsEmail(String memberEmail);

	int updatePw(MemberDTO member);

	MemberDTO selectOneMember(String memberEmail);

	MemberDTO socialLogin(String userEmail);
	
	int isSocial(String email);
	
	MemberDTO selectMemberInfo(String memberNickname);

	int updateMemberNewFile(MemberDTO member);
	
	int updateMemberPresFile(MemberDTO member);

	int updateMemberDelFile(MemberDTO member);
	
	String selectDelImg(String memberNickname);
	
	int deleteMember(MemberDTO memberNickname);

	int insertDelMember(MemberDTO member);





}
