package kr.co.iei.admin.controller;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import kr.co.iei.admin.model.dto.CompanyDTO;
import kr.co.iei.admin.model.dto.EventDTO;
import kr.co.iei.admin.model.service.AdminService;
import kr.co.iei.review.model.dto.ReviewDTO;
import kr.co.iei.review.model.service.ReviewService;
import kr.co.iei.util.FileUtils;

@CrossOrigin("*")
@RestController
@RequestMapping(value="/admin")
public class AdminController {
	@Autowired
	private FileUtils fileUtils;
	@Autowired
	private ReviewService reviewService;
	@Autowired
	private AdminService adminService;
	@Value("${file.root}")
	private String root;
	@GetMapping("/company")
	private ResponseEntity<CompanyDTO> selectCompanyInfo(){
		CompanyDTO company = adminService.selectCompanyInfo();
		return ResponseEntity.ok(company); 
	}
	@PatchMapping("/company")
	private ResponseEntity<Integer> updateCompany(@RequestBody CompanyDTO company){
		int result = adminService.updateCompanyInfo(company);
		return ResponseEntity.ok(result);
	}
	
	@GetMapping("/event/{month}")
	public ResponseEntity<List> selectMonthEvent(@PathVariable String month){
		List eventList = adminService.selectMonthEvent(month);
		return ResponseEntity.ok(eventList);
	}
	@GetMapping("/event/onGoing")
	public ResponseEntity<List> selectOnGoingEvent(@RequestParam String date){
		List eventList = adminService.selectOnGoingEvent(date);
		return ResponseEntity.ok(eventList);
	}
	@GetMapping("/event/end")
	public ResponseEntity<List> selectEndEvent(@RequestParam String date){
		System.out.println(date);
		List eventList = adminService.selectEndEvent(date);
		return ResponseEntity.ok(eventList);
	}
	
	@PostMapping("/event")
	public ResponseEntity<Integer> insertEvent(@ModelAttribute EventDTO event,@ModelAttribute MultipartFile img){
		String savepath = root +"/event/thumb/";
		String filepath = fileUtils.upload(savepath, img);
		event.setEventImg(filepath);
		int result = adminService.insertEvent(event);
		return ResponseEntity.ok(result);
	}
	@PatchMapping("/event/{eventNo}")
	public ResponseEntity<Integer> updateEvent(@ModelAttribute EventDTO event,@ModelAttribute MultipartFile img,@PathVariable int eventNo){
		if(img!=null) {
			String savepath = root +"/event/thumb/";
			String filepath = fileUtils.upload(savepath, img);
			event.setEventImg(filepath);
		}
		String filepath = adminService.updateEvent(event);
		if(filepath!=null){
			File file = new File(root+"/event/thumb/"+filepath);
			if(file.exists()) {
				file.delete();
			}
		}
		return ResponseEntity.ok(1);
	}
	@DeleteMapping("/event/{eventNo}")
	public ResponseEntity<Integer> deleteEvent(@PathVariable int eventNo){
		String filepath = adminService.deleteEvent(eventNo);
		File file = new File(root+"/event/thumb/"+filepath);
		if(file.exists()) {
			file.delete();
		}
		return ResponseEntity.ok(1);
	}
	@GetMapping("/report")
	public ResponseEntity<List> reportedReview(){
		List reportedReviews = reviewService.selectReportedReview();
		return ResponseEntity.ok(reportedReviews);
	}
}
