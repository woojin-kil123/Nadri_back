package kr.co.iei.plan.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.iei.plan.model.dto.ItineraryDTO;
import kr.co.iei.plan.model.dto.PlanDTO;
import kr.co.iei.plan.model.service.PlanService;

@CrossOrigin("*")
@RestController
@RequestMapping(value="/plan")
public class PlanController {
	@Autowired
	private PlanService planService;
	
	//플래너 진입 시 새 플래너인지, 기존 플래너인지 체크 (+ 자기 그룹의 플래너가 맞는지도 체크)
	@GetMapping(value="/verify/{planNo}")
	public ResponseEntity<Map> verifyPlan(@RequestHeader("Authorization") String refreshToken, @PathVariable int planNo){
		//plan_status가 1(공개 상태)이거나, 해당 plan의 plan_group에 대하여 소속되어 있는 계정인지
		//즉, "열람 권한"이 있는지 체크
		PlanDTO plan = planService.verifyPlan(refreshToken, planNo);
		
		//열람 권한이 없다면 취소
		if(plan == null) {
			return ResponseEntity.ok(null);
		}

		//열람 권한이 있다면, 반환할 map을 구성: 플랜 정보 + 저장된 여행지들 + 수정/삭제 권한 여부 
		Map<String, Object> map = new HashMap<>();
		map.put("plan", plan);
		List list = planService.selectPlanItineraries(planNo);
		map.put("itineraries", list);
		boolean isOwner = planService.isPlanOwner(refreshToken, planNo);
		map.put("isOwner", isOwner);
		
		return ResponseEntity.ok(map);
	}
	
	//유저 마커 반경 내 장소 데이터 수집
	@GetMapping(value="/nearby")
	public ResponseEntity<List> selectNearby(@RequestParam double lat, @RequestParam double lng, @RequestParam int radius){
		List list = planService.selectNearby(lat, lng, radius);
		return ResponseEntity.ok(list);
	}
	
	//플래너 저장
	@PostMapping(value="/save")
	public ResponseEntity<Boolean> savePlanner(@RequestBody Map<String, Object> planData) throws JsonMappingException, JsonProcessingException{
		ObjectMapper om = new ObjectMapper();
		PlanDTO plan = om.convertValue(planData.get("tripPlanData"), PlanDTO.class);
		
		List raw = (List) planData.get("itineraryList");
		List<ItineraryDTO> list = new ArrayList<>();
		for(Object obj : raw) {
			Map<String, Object> map = (Map<String, Object>) obj;
			ItineraryDTO i = new ItineraryDTO();
			i.setItineraryDate((String) map.get("itineraryDate"));
			i.setStartLocation(map.get("startLocation") == null ? 0 : (Integer) map.get("startLocation"));
			i.setTransport((String) map.get("transport"));
			i.setEndLocation((Integer) map.get("endLocation"));
			i.setItineraryOrder((Integer) map.get("itineraryOrder"));
			list.add(i);
		}
		System.out.println(list);
		boolean result = planService.insertPlan(plan, list);
		
		return ResponseEntity.ok(result);
	}
	
}
