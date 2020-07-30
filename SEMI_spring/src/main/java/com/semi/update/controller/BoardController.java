package com.semi.update.controller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.semi.update.All.pagination.OraclePagination;
import com.semi.update.All.util.DownloadFileUtils;
import com.semi.update.All.util.UploadFileUtils;
import com.semi.update.All.util.Util;
import com.semi.update.member.board.biz.BoardBiz;
import com.semi.update.member.board.comment.dto.CommentDto;
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
	public String boardMain(Model model, @ModelAttribute BoardDto dto, @RequestParam(defaultValue = "1") int currentPage) {
		
		//1) 전체 게시물 게수 가져오기
		int totalBoardCount = boardBiz.getTotalBoard(dto);	// 전체게시물 수  or 검색한 게시물 수
		
		/*2) 페이징 클래스 >> 쿼리에 필요한 시작페이지 번호, 끝 페이지 번호를 계산해서 가지고 있음  */
		OraclePagination pagination = new OraclePagination(totalBoardCount, currentPage);	// 전체 게시물 수, 현재 페이지 (== 요청된 페이지) 
		logger.info("board main page => [페이징] OraclePagination : " + pagination );
		
		
		//3) boardDto에 시작 페이지, 끝 페이지 추가
		dto.setStartBoardNo(pagination.getStartBoardNo());
		dto.setEndBoardNo(pagination.getEndBoardNo());
		
		// top N 쿼리를 사용하여 게시물 리스트 가져오기 
		List<BoardDto> list = boardBiz.boardList(dto);
		logger.info("board main page => 페이징 처리된 boardDto 리스트 개수 : " + list.size());
		for(BoardDto boardDto : list) {
			if(boardDto.getBoardTitle() != null) {
				boardDto.setBoardTitle(Util.omit(16, boardDto.getBoardTitle()));
			} 
			if(boardDto.getBoardContent() != null) {
				boardDto.setBoardContent(Util.omit(220, boardDto.getBoardContent()));
			}
		}
		
		model.addAttribute("list", list);
		model.addAttribute("pagination", pagination);
		return "board/BOARD_BoardMain";
	}

	// 글작성
	@RequestMapping(value = "/write.do")
	public String boardWrite() {
		logger.info("board write page");
		return "board/BOARD_boardwrite";
	}

	// 글 작성 비동기 멀티 이미지 업로드
	@ResponseBody
	@RequestMapping(value = "/AjaxFileUplod.do", method = RequestMethod.POST)
	public Map<String, Object> AjaxFileUplod(@ModelAttribute("fileArr") MultipartFile[] fileArr, BoardDto boardDto, HttpSession session) throws IOException {
		logger.info("[ajax] 본문 업로드 파일 개수 => " + fileArr.length);
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
			if(file.getSize() != 0) { // 파일이 있다면
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
				imgNames = imgNames + "??" + imgNameArr[i];
			}
		}
		boardDto.setImgPath(imgNames);
		
		int res = boardBiz.insertImg(boardDto);
		
		if(res > 0) {
			logger.info("Board img 추가 성공");
			
			// #4 방금 추가한 boardNo 알아낸다.
			String boardNo = boardBiz.getBoardNo(boardDto);
			// #5 여기까지 성공 헀다면 output를 만들어 보낸다 
			if(boardNo != null || boardNo.equals("")) {
				output.put("imgSrcArr", imgNameArr);
				output.put("msg", "success");
				output.put("boardNo", boardNo);		
			}
		}
		return output;
	}

	// 글쓰기 완료
	@RequestMapping(value="/writeRes.do", method = RequestMethod.POST)
	public String boardWriteRes(Model model, @ModelAttribute BoardDto boardDto, HttpSession session) throws IOException {
		
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
		
		if (fileArr[0].getSize() != 0) {	// 들어온 파일이 있다면
			//폴더 생성 >> fileUploadPath + /id/yyyy/mm/dd/ >> 있으면 pass
			String id_ymdPath = UploadFileUtils.calcPath(fileUploadPath, id);
			
			//파일 업로드
			for(int i = 0; i<fileArr.length; i++) {
				if (i == 0) {
					String temp = UploadFileUtils.fileUpload(fileUploadPath, fileArr[i].getOriginalFilename(),  fileArr[i].getBytes(), id_ymdPath);
					fileNames = fileUploadPath + id_ymdPath + File.separator + temp;
				} else {
					String temp = UploadFileUtils.fileUpload(fileUploadPath, fileArr[i].getOriginalFilename(),  fileArr[i].getBytes(), id_ymdPath);
					fileNames = fileNames + "??" + fileUploadPath + id_ymdPath + File.separator + temp;
				}
			}
		}
		
		// id, fileNames(업로드한 파일명) dto 추가
		boardDto.setId(id);
		boardDto.setFilePath(fileNames);
		
		// DB 추가
		if(boardDto.getBoardNo() == 0) {
			logger.info("board Write Res => [기존 이미지 없음] Board insert " + boardDto);
			
			int insertRes = boardBiz.insertNoImgBoard(boardDto);
			
			if(insertRes > 0) {
				logger.info("board Write Res => [추가 성공] Board insert success");
				return "redirect:/board/main.do";
			} else {
				logger.info("board Write Res => [추가 실패] Board insert fail");
				// date 포멧 변환 
				// title, content 길이 변경
				model.addAttribute("boardDto",boardDto);
				return "redirect:/board/write.do";
			}
		// DB 수정	
		} else {
			logger.info("board Write Res => [기존 이미지 있음] Board update" + boardDto);
			
			int updateRes = boardBiz.updateRestContent(boardDto);
			
			if(updateRes > 0) {
				logger.info("board Write Res => [수정 성공] Board update success");
				return "redirect:/board/main.do";
			} else {
				logger.info("board Write Res => [수정 실패] Board update fail");
				model.addAttribute("boardDto",boardDto);
				return "redirect:/board/write.do";
			}
		}
	}
	
	// 디테일 + 뎃글
	@RequestMapping(value="/detail.do", method = RequestMethod.GET)
	public String boardDetail(Model model, @RequestParam("boardNo") int boardNo, @RequestParam(defaultValue = "1") int currentPage, CommentDto commentDto) {
		logger.info("board detail page");

		BoardDto boardDto = boardBiz.selectOne(boardNo);
		if(boardDto.getFilePath() != null) {
			if(boardDto.getFilePath().contains("??")) {
				String[] fileNames = boardDto.getFilePath().split("\\?\\?");  // "\\" 두개를 붙이는 이유는  Meta character라서 정규식을 기반으로 구현한 메서드에 그대로 사용 불가하다.
				/* 
				 	# Meta character: / ? *
				 	 정규 표현식에는 특별한 의미를 없애고 문자 그대로 표현식 내에서 처리하기 위해 이스케이프해야하는 14 개의 메타 문자
				 */
				boardDto.setFilePath("첨부된 파일 " + fileNames.length +"개 ");
			} else {
				boardDto.setFilePath("첨부된 파일 1개 ");
			}
		}
		
		/*
		// 답글 dto에 부모글 시퀀스 추가
		commentDto.setBoardNo(boardNo);
		// 부모글의 답글 총개수 
		int totalComment = commentBiz.count_commentList(commentDto); 
		
		// 페이징 객체 생성(화면에 출력할 답글수(10, 출력될 페이지(여기서 필요없음), 총개수, 현재 페이지(1)
		OraclePagination pagination = new OraclePagination(10, 10, totalComment, currentPage);
		
		// 페이징 시작번호
		commentDto.setStartBoardNo(pagination.getStartBoardNo());
		// 페이징 끝번호
		commentDto.setEndBoardNo(pagination.getEndBoardNo());
		
		// 현재 게시물의 댓글 가져오기
		List<CommentDto> commentList = commentBiz.commentList(commentDto);
		
		model.addAttribute("comment", commentList);
		*/
		model.addAttribute("board", boardDto);
		return "board/BOARD_boardDetail";
	}
	
	// 첨부파일 확인
	@RequestMapping(value="/fileDetail.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> fileDown(@RequestParam("boardNo") int boardNo, BoardDto boardDto) {
		Map<String, Object> output = new HashMap<String, Object>();
		
		boardDto = boardBiz.selectOne(boardNo);
		if(boardDto.getFilePath() != null) {
			String fileFullNames = boardDto.getFilePath(); 
			if(fileFullNames.contains("??")) {
				String[] fileFullNameArr = fileFullNames.split("\\?\\?");  // "\\" 두개를 붙이는 이유는  Meta character라서 정규식을 기반으로 구현한 메서드에 그대로 사용 불가하다.
				String[] fileNameArr = new String[fileFullNameArr.length];
				for(int i = 0; i < fileFullNameArr.length; i++) {
					int index = fileFullNameArr[i].lastIndexOf("_") + 1;
					fileNameArr[i] = fileFullNameArr[i].substring(index, fileFullNameArr[i].length());
				}
				output.put("msg", "success");
				output.put("fileName", fileNameArr);
				
			} else {
				// 파일이 1개 일때
				int index = boardDto.getFilePath().lastIndexOf("_") + 1;
				String fileName = fileFullNames.substring(index, fileFullNames.length());
				output.put("msg", "success");
				output.put("fileName", fileName);
			}
		} else {
			output.put("msg", "fail");
		}
		
		return output;
	}
	
	// 파일 다운로드
	@RequestMapping(value="/fileDown.do", method = RequestMethod.POST)
	@ResponseBody
	public byte[] fileDown(HttpServletRequest request, HttpServletResponse response,@RequestParam("fileName") String fileName ,@RequestParam("boardNo") int boardNo, BoardDto boardDto) throws UnsupportedEncodingException {
		logger.info("board file down");
		byte[] down = null;
		String outFilePath = "";
		
		boardDto = boardBiz.selectOne(boardNo);
		if(boardDto.getFilePath() != null) {
			String fileFullNames = boardDto.getFilePath(); 
			if(fileFullNames.contains("??")) {
				String[] fileFullNameArr = fileFullNames.split("\\?\\?");  // "\\" 두개를 붙이는 이유는  Meta character라서 정규식을 기반으로 구현한 메서드에 그대로 사용 불가하다.
				for(int i = 0; i < fileFullNameArr.length; i++) {		
					int index = fileFullNameArr[i].lastIndexOf("_") + 1;	// 뒤에서 처음으로 _가 나오는 인덱스 번호를 찾는다.
					String tempFileName = fileFullNameArr[i].substring(index, fileFullNameArr[i].length());	// 원본 파일명을 가져온다.
					if(fileName.equals(tempFileName)) {			// 다운요청한 파일명과 일치하는 파일명을 찾는다
						outFilePath = fileFullNameArr[i];
					}
				}
			} else {
				int index = fileFullNames.lastIndexOf("_") + 1;	// 뒤에서
				String tempFileName = fileFullNames.substring(index, fileFullNames.length());
				if(fileName.equals(tempFileName)) {			// 다운요청한 파일명과 일치하는 파일명을 찾는다
					outFilePath = fileFullNames;
				}
			}
			// 단일 파일 다운로드
			logger.info("[fileDown.do] => 다운로드 파일명 : " + outFilePath);
			File file = new File(outFilePath);
			down = DownloadFileUtils.file_toByte(file);	// == FileCopyUtils.copyToByteArray(file);	#스프링에서 제공하는 파일 다운로드 유틸 
				
			String filename = new String(file.getName().getBytes("utf-8"), "8859_1");	 			// 파일 이름을 "utf-8"의 바이트 코드로 변환, 8859_1 인코딩 설정
			response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");	
			response.setContentType("application/octet-stream"); 									// # application/octet-stream는 다른 모든 경우를 위한 기본값입니다. ,알려지지 않은 파일 타입은 이 타입을 사용해야 합니다
			response.setContentLength(down.length);
			
			//HttpHeaders headers = new HttpHeaders();  >>> 스프링에서 지원하는 Http 헤더설정 클래스 
		}
		return down;
	}
	
	// 수정하기 페이지
	@RequestMapping(value="/update.do", method = RequestMethod.GET)
	public String update(Model model, @RequestParam("boardNo") int boardNo) {
		logger.info("board update page go boardNO : " + boardNo);
		BoardDto boardDto = boardBiz.selectOne(boardNo);
		model.addAttribute("board", boardDto);
		
		return "board/BOARD_boardupdate";
	}
	
	// 수정하기 이미지 업로드
	@RequestMapping(value="/AjaxFileUpdate.do", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> AjaxFileUpdate(@ModelAttribute("fileArr") MultipartFile[] fileArr, @ModelAttribute("boardNo") int boardNo,BoardDto boardDto, HttpSession session) throws IOException {
		logger.info("[ajax] 글 수정 본문 업로드 파일 개수 => " + fileArr.length);
		Map<String, Object> output = new HashMap<String, Object>();
		output.put("msg", "fail");		// 디폴트 fail
		
		// 멘토나 멘티 id
		String id = "";

		// #1 유저id, 년, 월, 일 폴더 생성
		String id_ymdPath = "";
		MentorDto mentorDto = (MentorDto) session.getAttribute("login");
		if(mentorDto != null) {
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
			if(file.getSize() != 0) { // 파일이 있다면
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
		// #3 DB저장 (id, 시퀀스_pk, 썸네일이미지명, 이미지명) 저장
		boardDto.setId(id);
		boardDto.setBoardNo(boardNo);
		boardDto.setThumbnail(thumbImgName);
		String imgNames = "";
		for(int i = 0; i<imgNameArr.length; i++) {
			if(i == 0) {
				imgNames = imgNameArr[0];
			} else {
				imgNames = imgNames + "??" + imgNameArr[i];
			}
		}
		boardDto.setImgPath(imgNames);
		
		// **수정전 이전 Dto 가져오기
		BoardDto oldBoardDto = boardBiz.selectOne(boardNo);
		
		int res = boardBiz.updateImg(boardDto);
		
		if(res > 0) {
			logger.info("Board img 수정 성공");
			
			// #4  **기존에 있던 이미지 삭제
			logger.info("board Update Res => 수정후 글의 이전 이미지 모두 삭제");
			String[] formerImgPathArr = oldBoardDto.getImgPath().split("\\?\\?");	// .split() : 결과를 배열로 리턴한다. 문자열을 나눌 기준이 없을 경우 문자열을 그대로 배열의 0번지에 넣어 리턴한다.(즉 리턴되는 배열의 크기는  항상 1이상이다.)	
											  
			// **파일 삭제 코드 : while문을 사용하여 파일삭제가 실패한 경우에 재실행 코드 구현
			int i = 0;
			while(i<formerImgPathArr.length) {
				String fileName = Util.toAbsolutePath(formerImgPathArr[i], "C:\\git\\semi\\SEMI_spring\\src\\main\\webapp\\", 8);
				if(Util.fileDelete(fileName)) {
					i++;	// 파일 삭제 성공인 경우에만 i++ 실행
				}
			}
			// #5 여기까지 성공 헀다면 output를 만들어 보낸다 
			output.put("imgSrcArr", imgNameArr);
			output.put("msg", "success");
		}		
		logger.info("[AjaxFileUpdate.do] => output : " + output);
		return output;
	}
	
	
	@RequestMapping(value="/updateRes", method = RequestMethod.POST)
	public String updateBoard(Model model, @ModelAttribute BoardDto boardDto, HttpSession session) throws IOException {
		logger.info("board Update Res => " + boardDto);
		
		// #1 세션에서 id 찾기
		MentorDto mentorDto = (MentorDto) session.getAttribute("login");
		String id = "";
		if(mentorDto != null) {
			id = mentorDto.getId();
		} else {
			MenteeDto menteeDto = (MenteeDto) session.getAttribute("login");
			id = menteeDto.getId();
		}
		
		// #2 첨부파일 업로드
		MultipartFile[] fileArr = boardDto.getFile();
		String fileNames = "";  
		
		if (fileArr[0].getSize() != 0) {	// 들어온 파일이 있다면
			//폴더 생성 >> fileUploadPath + /id/yyyy/mm/dd/ >> 있으면 pass
			String id_ymdPath = UploadFileUtils.calcPath(fileUploadPath, id);
			
			//파일 업로드
			for(int i = 0; i<fileArr.length; i++) {
				if (i == 0) {
					String temp = UploadFileUtils.fileUpload(fileUploadPath, fileArr[i].getOriginalFilename(),  fileArr[i].getBytes(), id_ymdPath);
					fileNames = fileUploadPath + id_ymdPath + File.separator + temp;
				} else {
					String temp = UploadFileUtils.fileUpload(fileUploadPath, fileArr[i].getOriginalFilename(),  fileArr[i].getBytes(), id_ymdPath);
					fileNames = fileNames + "??" + fileUploadPath + id_ymdPath + File.separator + temp;
				}
			}
		}
		
		// id, fileNames(업로드한 파일명) dto 추가
		boardDto.setId(id);
		if(boardDto.getFilePath() == null) {
			// 업로드된 첨부 파일이 없다면 기존 경로를 사용
			BoardDto temp = boardBiz.selectOne(boardDto.getBoardNo());
			boardDto.setFilePath(temp.getFilePath());
		} else {
			// 업로드된 첨부 파일이 있다면 업로드한 파일 경로를 새로 추가
			boardDto.setFilePath(fileNames);
		}
		
		// content 안에 img태그가 없을 경우 >>> DB 이미지 썸내일 칼럼 삭제
		if(!Util.isImgTag(boardDto.getBoardContent())) {	
			int noImgUpdateRes = boardBiz.updateNoImgBoard(boardDto);
			if(noImgUpdateRes > 0) {
				logger.info("board Update Res => [No img 수정 성공] Board update success");
				
				// 썸내엘 본문 이미지 파일 삭제 코드 추가 예정
				
				return "redirect:/board/detail.do?boardNo=" + boardDto.getBoardNo();
			} else {
				logger.info("board Update Res => [No img 수정 성공] Board update fail");
				return "redirect:/board/update.do";
			}
		} else {
			// 이미지 있을경우 DB 수정
			BoardDto oldBoardDto = boardBiz.selectOne(boardDto.getBoardNo());
			int res = boardBiz.updateBoard(boardDto);
			if(res > 0) {
				logger.info("board Update Res => [글 수정 성공] Board update success");
				return "redirect:/board/detail.do?boardNo=" + boardDto.getBoardNo();
			} else {
				logger.info("board Update Res => [글 수정 실패] Board update fail");
				model.addAttribute("boardDto",boardDto);
				return "redirect:/board/update.do";
			}
		}
	}
	
	// 좋아요
	
	// 자신이 쓴 글 리스트 보기
	@RequestMapping(value = "/myMain.do")									
	public String MyboardMain(Model model, @ModelAttribute BoardDto dto, @RequestParam(defaultValue = "1") int currentPage, HttpSession session) {
		
		// #1 세션에서 id 찾기
		MentorDto mentorDto = (MentorDto) session.getAttribute("login");
		String id = "";
		if(mentorDto != null) {
			id = mentorDto.getId();
		} else {
			MenteeDto menteeDto = (MenteeDto) session.getAttribute("login");
			id = menteeDto.getId();
		}
		
		dto.setId(id);
		
		// 1) 유저가 작성한 전체 게시물 게수 가져오기
		int totalBoardCount = boardBiz.getMyTotalBoard(dto);	// 전체게시물 수  or 검색한 게시물 수
		
		/* 2) 페이징 클래스 >> 쿼리에 필요한 시작페이지 번호, 끝 페이지 번호를 계산해서 가지고 있음  */
		OraclePagination pagination = new OraclePagination(totalBoardCount, currentPage);	// 전체 게시물 수, 현재 페이지 (== 요청된 페이지) 
		logger.info("My board main page => [페이징] OraclePagination : " + pagination );
		
		
		//3) boardDto에 시작 페이지, 끝 페이지 추가
		dto.setStartBoardNo(pagination.getStartBoardNo());
		dto.setEndBoardNo(pagination.getEndBoardNo());
		
		// top N 쿼리를 사용하여 유저가 작성한 게시물 리스트 가져오기 
		List<BoardDto> list = boardBiz.myBoardList(dto);
		logger.info("My board main page => 페이징 처리된 boardDto 리스트 개수 : " + list.size());
		for(BoardDto boardDto : list) {
			if(boardDto.getBoardTitle() != null) {
				boardDto.setBoardTitle(Util.omit(16, boardDto.getBoardTitle()));
			} 
			if(boardDto.getBoardContent() != null) {
				boardDto.setBoardContent(Util.omit(220, boardDto.getBoardContent()));
			}
		}
		
		model.addAttribute("list", list);
		model.addAttribute("pagination", pagination);
		return "board/BOARD_myBoardMain";
	}
	
	/*
	 * @RequestParam("boardNo") int boardNo == request.getParameter("boardNo");
	 * 		>>> 추가 기능
	 * 		1. @RequestParam(value = "boardNo", required = false)	>>>	required = false : 전달받은 값이 없어도 예외를 발생하지 않게 한다    
	 * 		2. 배열로 받기  
	 * 			>>> 서블릿에서 form태그에서 같은 이름으로 전송시(멀티삭제) getParameterValues()을 사용하여 배열로 받을수 있었다.
	 * 				즉 @RequestParam은 form태그의 같은 name을 배열로 해당 기능도 지원한다.
	 * 
	 * @RequsetAttribute("boardDto") == request.getAttribute("boardDto");
	 * 
	 * @ModelAttribute("boardDto") == request.getAttribute("boardDto");
	 * 		>>> form로 전송시 dto의 setter와 일치하는 name이 자동으로 바인딩된다. 주로 post전송시 많이 사용된다(ajax의 post전송시 사용해야함)
	 *		>>> 이 어노테이션의 단점은 중복이름을 배열로 받는것을 지원하지 않는다는 것이다. 		
	 */
	
	// 자신글 하나 삭제
	@RequestMapping(value="/deleteOne.do", method=RequestMethod.GET)
	public String deleteOne(@RequestParam("boardNo") int[] boardNo) {
		// 삭제전 글 가져오기
		BoardDto boardDto = boardBiz.selectOne(boardNo[0]);
		// 글삭제
		int res = boardBiz.multiBoardDelete(boardNo);
		if(res > 0) {
			logger.info("My board delete one success : " + res);
			// 파일 삭제
			fileDelete(boardDto);
			return "redirect:/board/main.do";
		} else {
			logger.info("My board delete one fail : " + res);
			return "redirect:/board/detail.do?boardNo=" + boardNo[0];
		}
		
	}
	
	
	// 자신이 쓴 글 멀티 삭제
	@RequestMapping(value="/multiDelete.do", method=RequestMethod.POST)
	public String multDelete(HttpSession session, @RequestParam("boardNoArr") int[] boardNoArr){
												/* ##중요 >>> @ModelAttribute는 중복된 이름을 배열로 받는것을 지원하지 않는다.(객체를 리스트나 배열로 받으려면 >> class를 따로 만들어야 한다.) */  
		
		// 현재 boardNoArr 배열의 크기많큼 DB에 seletOne 요청을 하고있음	>> selectList로 바꿔야함 
		BoardDto[] boardDtoArr = new BoardDto[boardNoArr.length]; 
		
		for(int i = 0; i<boardNoArr.length; i++) {
			boardDtoArr[i] = boardBiz.selectOne(boardNoArr[i]);
		}
		
		int res = boardBiz.multiBoardDelete(boardNoArr);
		
		// res는 삭제한 로우 개수를 반환받는다. 2개를 삭제했으면  : 2 	>> 트렌젝션 처리해야함
		if(res > 0) {
			logger.info("My board multiDelete success : " + res);
			int count = 0;
			// 삭제된 게시글 하나씩 파일 삭제
			for(BoardDto boardDto : boardDtoArr) {
				logger.info("board multiDelete [총 게시물  " + boardDtoArr.length + "개, 그 중 " + (count + 1) + "번째 글 파일 삭제 준비]");
				// 파일 삭제 메서드
				fileDelete(boardDto);				
				count++;
			}
			logger.info("board multiDelete => 글 멀티 삭제 후 글의 이미지 모두 삭제 [성공]");
			return "redirect:/board/myMain.do";
		} else {
			logger.info("My board multiDelete fail : " + res);
			return "redirect:/board/myMain.do";
		}
	}
	
	
	// 게시판 글 삭제시 해당글 관련 파일 삭제 메서드
	public void fileDelete(BoardDto boardDto) {
		logger.info("board deleteOne [파일 삭제 메서드 실행] ====================================> 글 삭제 후 해당글 파일(이미지, 첨부파일) 모두 삭제 실행 ");
		
		// 각각의 파일명 없을 경우 처리 == NullPointerException 처리
		String[] relativeImgPathArr = new String[0];
		String[] filePathArr = new String[0];
		String relativeThumbnailPath = "";
		
		if(boardDto.getImgPath() != null) {
			logger.info("board deleteOne [파일 삭제 준비] : 본문에 삭제해야하는 이미지 명 : " + boardDto.getImgPath());
			// 글에 속한 파일 > 상대경로
			relativeImgPathArr = boardDto.getImgPath().split("\\?\\?");		
		} 
		if(boardDto.getFilePath() != null) {
			logger.info("board deleteOne [파일 삭제 준비] : 본문에 삭제해야하는 첨부파일 명 : " + boardDto.getFilePath());
			// 첨부파일 > 절대경로
			filePathArr = boardDto.getFilePath().split("\\?\\?");
		}
		if(boardDto.getThumbnail() != null) {
			logger.info("board deleteOne [파일 삭제 준비] : 본문에 삭제해야하는 썸네일 명 : " + boardDto.getThumbnail());
			// 썸내일 > 상대경로
			relativeThumbnailPath = boardDto.getThumbnail();
		}
		// **파일 삭제 코드 : while문을 사용하여 파일삭제가 실패한 경우에 재실행 코드 구현
		// 1). 본문 이미지 삭제 
		int i = 0;
		while(i<relativeImgPathArr.length) {
			if(i<relativeImgPathArr.length) {
				// 상대경로  > 절대경로로 변환
				String imgfileName = Util.toAbsolutePath(relativeImgPathArr[i], "C:\\git\\semi\\SEMI_spring\\src\\main\\webapp\\", 8);
				logger.info("board deleteOne [이미지 파일 삭제 실행] : 글의 [본문 이미지] 모두 삭제 실행 파일명 : " + imgfileName);
				if(Util.fileDelete(imgfileName)) {
					i++;	// 파일 삭제 성공인 경우에만 i++ 실행
				}
			}
		}
		// 2) 첨부파일 삭제
		int j = 0;
		while(j<filePathArr.length) {
			if(j < filePathArr.length) {
				logger.info("board deleteOne [첨부파일 삭제 실행] : 글의 [첨부파일] 모두 삭제 실행 파일명: " + filePathArr[j]);
				if(Util.fileDelete(filePathArr[j])) {		// out of Range 예외 때문에 해야함
					j++;
				}
			}
		}
		// 3) 마지막 썸네일 있을 경우 삭제
		if(!relativeThumbnailPath.equals("")) {
			String thumbnailPath = Util.toAbsolutePath(relativeThumbnailPath, "C:\\git\\semi\\SEMI_spring\\src\\main\\webapp\\", 8);
			logger.info("board deleteOne [썸네일 삭제 실행] : 글 삭제 후 [썸내일] 삭제 실행 파일명 : " + thumbnailPath);
			if(Util.fileDelete(thumbnailPath)) {		// out of Range 예외 때문에 해야함
			}
		}
		
		logger.info("board deleteOne [파일 메서드 종료] ====================================> 해당 글의 파일 모두 삭제 [성공] \n\n");
	}
	
}
