package kr.co.iei.review.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import kr.co.iei.review.model.dto.ReportDTO;
import kr.co.iei.review.model.service.ReportService;

import org.springframework.web.bind.annotation.RestController;
import kr.co.iei.review.model.dto.ReportDTO;


@CrossOrigin("*")
@RestController
@RequestMapping(value="/report")
public class ReportController {
	@Autowired
	private ReportService reportService;
	@PostMapping(value = "/")
	public ResponseEntity<Integer> insertreport(@RequestBody ReportDTO reportDTO){
	int result = reportService.insertReport(reportDTO);
		 return ResponseEntity.ok(result);
	}
}
