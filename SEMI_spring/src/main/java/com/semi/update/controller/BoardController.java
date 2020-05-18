package com.semi.update.controller;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.semi.update.All.util.UploadFileUtils;
import com.semi.update.member.board.biz.BoardBiz;
import com.semi.update.member.board.dto.BoardDto;
import com.semi.update.member.dto.MenteeDto;
import com.semi.update.member.dto.MentorDto;

@Controller
@RequestMapping("/board")
public class BoardController {

	private Logger logger = LoggerFactory.getLogger(BoardController.class);
	
	@Autowired
	private BoardBiz boardBiz;
	
	@Resource(name="boardImgUploadPath")
	private String imgUploadPath;
	
	@Resource(name="boardFileUploadPath")
	private String fileUploadPath;

	// 메인
	@RequestMapping(value = "/main.do")
	public String boardMain(Model model, @ModelAttribute BoardDto dto) {
		logger.info("board main page");
		List<BoardDto> list = boardBiz.boardList(dto);
		model.addAttribute("list", list);
		// 향후 페이징 처리
		
		return "board/BOARD_BoardMain";
	}

	// 글쓰기
	@RequestMapping(value = "/write.do")
	public String boardWrite() {
		logger.info("board write page");
		return "board/BOARD_boardwrite";
	}

	// 글취소 >> 이미지 저장으로 인해 저장된 글이 있다면 삭제
	@RequestMapping(value = "/writeCancel.do")
	public String boardWriteCancel() {
		logger.info("board write Cancel");

		// 작성하던 이미지 가져와서 삭제?

		return "redirect:/board/main.do";
	}

	// 썸머노트 이미지
	@ResponseBody
	@RequestMapping(value = "/imgUplod.do")
	public Map<String, Object> summernoteImgUpload(@ModelAttribute("file") MultipartFile file, BoardDto boardDto, HttpSession session) throws IOException {
		logger.info("[ajax] summernote img Upload : >>>>>>>>>>>>>>>>>>>>>  " + file);
		
		Map<String, Object> output = new HashMap<String, Object>();
		output.put("msg", "fail");		// 디폴트 fail
		output.put("boardNo", "0");		// 디폴트 0 >>> 게시판 insert 성공시 seq를 담는다
		
		// 멘토나 멘티 id
		String id;

		// #1 유저id, 년, 월, 일 폴더 생성
		String id_ymdPath = "";
		MentorDto mentorDto = (MentorDto) session.getAttribute("login");
		if(mentorDto != null) {
			logger.info("폴더생성");
			id = mentorDto.getId();
			// id_ymdPath 파일 경로 설정 >>> 유져id/년/월/일 
			id_ymdPath = UploadFileUtils.calcPath(imgUploadPath, id);
		} else {
			MenteeDto menteeDto = (MenteeDto) session.getAttribute("login");
			id = menteeDto.getId();
			// id_ymdPath 파일 경로 설정 >>> 유져id/년/월/일
			id_ymdPath = UploadFileUtils.calcPath(imgUploadPath, id);
		}
	
		
		// #2 파일 저장후 성공시 "view에서 불러올 경로 + 파일명" 배열에 담는다[썸내일 제외 원본이미지], 썸내일은 따로 "view에서 불러올 경로 + 파일명"을 String에 담는다. >> 향후 실패 처리시를 만들어야함
		String imgName = "";
		String thumbImgName = "";

		if(file != null) {
			// 파일과 썸내일 생성 >> 썸내일이름[0], 원본파일이름[1] 배열로 리턴
			String[] tempName =  UploadFileUtils.imgUploadAndThumb(imgUploadPath, file.getOriginalFilename(), file.getBytes(), id_ymdPath);
			// DB에 저장할 경로 : /update/resources/img/board/img + / + 유져id/년/월/일 + /s/ + / 썸내일 파일명 
			thumbImgName = "/update/resources/img/board/img" + File.separator + id_ymdPath + "/s/"  + tempName[0];  
			// DB에 저장할 경로 : /update/resources/img/board/img + / + 유져id/년/월/일 + / + 파일명 
			imgName  = "/update/resources/img/board/img" + File.separator + id_ymdPath + File.separator + tempName[1];
		}
		// #3 DB저장 (id, 썸네일이미지명, 이미지명) 저장
		boardDto.setId(id);
		boardDto.setThumbnail(thumbImgName);
		boardDto.setImgPath(imgName);
		
		int res = boardBiz.insertImg(boardDto);
		
		if(res > 0) {
			logger.info("Board img 추가 성공");
			
			// #4 방금 추가한 boardNo 알아낸다.
			String boardNo = boardBiz.getBoardNo(boardDto);
			logger.info("Board >>>>>>>>>>>>>>>  추가한 BoardNo : " + boardNo);
			// #5 여기까지 성공 헀다면 output를 만들어 보낸다 
			if(boardNo != null || boardNo.equals("")) {
				output.put("img", imgName);
				output.put("msg", "success");
				output.put("boardNo", boardNo);		
			}
		}
		return output;
	}
		

	// 비동기 멀티 이미지 업로드
	@ResponseBody
	@RequestMapping(value = "/AjaxFileUplod.do")
	public Map<String, Object> AjaxFileUplod(@ModelAttribute("fileArr") MultipartFile[] fileArr, BoardDto boardDto, HttpSession session) throws IOException {
		logger.info("[ajax] Ajax File Uplod : >>>>>>>>>>>>>>>>>>>>>  " + fileArr);
		logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + fileArr.length);
		Map<String, Object> output = new HashMap<String, Object>();
		output.put("msg", "fail");		// 디폴트 fail
		output.put("boardNo", "0");		// 디폴트 0 >>> 게시판 insert 성공시 seq를 담는다
		
		// 멘토나 멘티 id
		String id;

		// #1 유저id, 년, 월, 일 폴더 생성
		String id_ymdPath = "";
		MentorDto mentorDto = (MentorDto) session.getAttribute("login");
		if(mentorDto != null) {
			logger.info("폴더생성");
			id = mentorDto.getId();
			// id_ymdPath 파일 경로 설정 >>> 유져id/년/월/일 
			id_ymdPath = UploadFileUtils.calcPath(imgUploadPath, id);
		} else {
			MenteeDto menteeDto = (MenteeDto) session.getAttribute("login");
			id = menteeDto.getId();
			// id_ymdPath 파일 경로 설정 >>> 유져id/년/월/일
			id_ymdPath = UploadFileUtils.calcPath(imgUploadPath, id);
		}
	
		
		// #2 파일 저장후 성공시 "view에서 불러올 경로 + 파일명" 배열에 담는다[썸내일 제외 원본이미지], 썸내일은 따로 "view에서 불러올 경로 + 파일명"을 String에 담는다. >> 향후 실패 처리시를 만들어야함
		String[] imgNameArr = new String[fileArr.length];
		String thumbImgName = "";

		int j = 0;
		for(MultipartFile file : fileArr) {
			if(file != null) {
				if(j == 0) {
					// 파일과 썸내일 생성 >> 썸내일이름[0], 원본파일이름[1] 배열로 리턴
					String[] tempName =  UploadFileUtils.imgUploadAndThumb(imgUploadPath, file.getOriginalFilename(), file.getBytes(), id_ymdPath);
					// DB에 저장할 경로 : /update/resources/img/board/img + / + 유져id/년/월/일 + /s/ + / 썸내일 파일명 
					thumbImgName = "/update/resources/img/board/img" + File.separator + id_ymdPath + "/s/"  + tempName[0];  
					// DB에 저장할 경로 : /update/resources/img/board/img + / + 유져id/년/월/일 + / + 파일명 
					imgNameArr[0] = "/update/resources/img/board/img" + File.separator + id_ymdPath + File.separator + tempName[1];
					
				} else {
					// 파일 생성
					String fileName = UploadFileUtils.fileUpload(imgUploadPath, file.getOriginalFilename(), file.getBytes(), id_ymdPath);
					// DB에 저장할 경로 : /update/resources/img/board/img + / + 유져id/년/월/일 + / + 파일명 
					imgNameArr[j] = "/update/resources/img/board/img" + File.separator + id_ymdPath + File.separator + fileName;
				}
				j++;
			} 
		}
		
		// #3 DB저장 (id, 썸네일이미지명, 이미지명) 저장
		boardDto.setId(id);
		boardDto.setThumbnail(thumbImgName);
		String imgNames = "";
		for(int i = 0; i<imgNameArr.length; i++) {
			if(i == 0) {
				imgNames = imgNameArr[0];
			} else {
				imgNames = imgNames + "&&" + imgNameArr[i];
			}
		}
		boardDto.setImgPath(imgNames);
		
		int res = boardBiz.insertImg(boardDto);
		
		if(res > 0) {
			logger.info("Board img 추가 성공");
			
			// #4 방금 추가한 boardNo 알아낸다.
			String boardNo = boardBiz.getBoardNo(boardDto);
			logger.info("Board >>>>>>>>>>>>>>>  추가한 BoardNo : " + boardNo);
			// #5 여기까지 성공 헀다면 output를 만들어 보낸다 
			if(boardNo != null || boardNo.equals("")) {
				output.put("imgArr", imgNameArr);
				output.put("msg", "success");
				output.put("boardNo", boardNo);		
			}
		}
		return output;
	}

	// 글쓰기 완료
	@RequestMapping(value="/writeRes.do", method = RequestMethod.POST)
	public String boardWriteRes(Model model, @ModelAttribute BoardDto boardDto, HttpSession session) throws IOException {
		logger.info("board Write Res >>>>>>>>>>>>>>>>>>>>> " + boardDto);
		
		MentorDto mentorDto = (MentorDto) session.getAttribute("login");
		String id = "";
		if(mentorDto != null) {
			id = mentorDto.getId();
		} else {
			MenteeDto menteeDto = (MenteeDto) session.getAttribute("login");
			id = menteeDto.getId();
		}
		
		MultipartFile[] fileArr = boardDto.getFile();
		String fileNames = "";  
		
		//폴더 생성 >> fileUploadPath + /id/yyyy/mm/dd/
		String id_ymdPath = UploadFileUtils.calcPath(fileUploadPath, id);
		
		//파일 업로드
		for(int i = 0; i<fileArr.length; i++) {
			String temp = UploadFileUtils.fileUpload(fileUploadPath, fileArr[i].getOriginalFilename(),  fileArr[i].getBytes(), id_ymdPath);
			fileNames = fileNames + "&&" + temp;
		}
		
		// id, fileNames(업로드한 파일명) dto 추가
		boardDto.setId(id);
		boardDto.setFilePath(fileNames);
		
		// DB 추가
		if(boardDto.getBoardNo() == 0) {
			logger.info("board Write Res >>>>>>>>>>>>>>>> [기존 이미지 없음] Board insert " + boardDto);
			
			int insertRes = boardBiz.insertNoImgBoard(boardDto);
			
			if(insertRes > 0) {
				logger.info("board Write Res >>>>>>>>>>>>>>>> [추가 성공] Board insert success");
				return "redirect:/board/main.do";
			} else {
				logger.info("board Write Res >>>>>>>>>>>>>>>> [추가 실패] Board insert fail");
				model.addAttribute("boardDto",boardDto);
				return "redirect:/board/write.do";
			}
		// DB 수정	
		} else {
			logger.info("board Write Res >>>>>>>>>>>>>>>> [기존 이미지 있음] Board update" + boardDto);
			
			int updateRes = boardBiz.updateRestContent(boardDto);
			
			if(updateRes > 0) {
				logger.info("board Write Res >>>>>>>>>>>>>>>> [수정 성공] Board update success");
				return "redirect:/board/main.do";
			} else {
				logger.info("board Write Res >>>>>>>>>>>>>>>> [수정 실패] Board update fail");
				model.addAttribute("boardDto",boardDto);
				return "redirect:/board/write.do";
			}
			
		}
	}
	
	// 넘어온 데이터에서 boardNo가 있으면 >> 기존 seq를 이용해 이미지가 추가되었을 경우 update
	// 넘어온 데이터에서 boardNo가 없으면>> 이미지가 추가되지 않았을 경우 insert

	// 디테일
	// >> 
	
	// 좋아요

	// 수정하기

	// 자신이 쓴 글 리스트 보기

	// 자신이 쓴 글 멀티 삭제
}
