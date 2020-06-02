package com.semi.update.All.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
	private static Logger logger = LoggerFactory.getLogger(Util.class);

	// 해당폴더 파일개수
	public static int getFileCount(String fileRoute) {
		int count = 0;
		File file = new File(fileRoute);
		for (File f : file.listFiles()) {
			if (f.isFile()) {
				count++;
			}
		}
		// System.out.println(count);
		return count;
	}
	
	// 해당폴더 파일경로+파일명 가져오기
	public static String[] getFilesName(String fileRoute) {
		File file = new File(fileRoute);
		int num = getFileCount(fileRoute);
		int index = 0;
		String[] fileNames = new String[num];
		for(File f : file.listFiles()) {		// listFiles() 해당 경로의 파일들과 폴더의 파일을 배열로 반환한다.
			if(f.isFile()) {			 		// 파일이면 true
				String name = f.getName();		// 파일이나 폴더의 이름을 넘겨준다
				fileNames[index] = name;
				index++;
			}
		}
		//System.out.println(Arrays.toString(arrS));
		return fileNames;
	}
	
	// 파일 삭제
	public static boolean fileDelete(String fileName) {
		logger.info("[Util] >>>>>>>>>>>> fileDelete() 삭제시도 파일명 : " + fileName);
		File file = new File(fileName);
		if(file.exists()) {
			if(file.delete()) {
				logger.info("[Util] >>>>>>>>>>>> fileDelete() 파일이 삭제 성공.");
				return true;
			} else {
				logger.info("[Util] >>>>>>>>>>>> fileDelete() 파일 삭제 실패");
				return false;
			}
		} else {
			logger.info("[Util] >>>>>>>>>>>> fileDelete() 파일이 존재하지 않습니다.");
			return true;
		}
	}
	
	// veiw에 표시될 문자열 길이 조절
	public static String omit(int length, String text) {
		text = removeTag(text);
		if(text.length() > length) {
			return text.substring(0, length) + "...";
		} else {
			return text; 
		}
	}
	
	// 정규식으로 html태그 제거 
	public static String removeTag(String html) {
		return html.replaceAll("<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(/)?>", "");
		// .replace() vs replaceAll() : replace는 정규표현식 사용불가,  replaceAll()은 정규표현식을 사용 가능하다.
	}
	
	//정규식으로 img 태그 찾기 
	public static boolean isImgTag(String input){
		Pattern pattern = Pattern.compile("<img[^>]*src=[\"']?([^>\"']+)[\"']?[^>]*>");
		Matcher matcher = pattern.matcher("<img src='img/Desert.jpg'>");	// <img src=''> src가 없다면 안됨
		if(matcher.matches()) return true;
		else return false;
	}
	
	// 게시판 img 상대경로 절대경로로 바꾸기
	public static String toAbsolutePath(String relativePath , String prefixPath, int subStringIndex) {
		return prefixPath + relativePath.substring(subStringIndex, relativePath.length());
	}
	
	
	// Date -> String (yyyyMMdd) 형식으로 변환
	public static String isString(Date date) {
		SimpleDateFormat transFormat = new SimpleDateFormat("yyyyMMdd");
		return transFormat.format(date);
	}

	// 한 자리수를 두 자리수로 변환
	public static String isTwo(String date) {
		return Integer.parseInt(date) < 10 ? "0" + date : date;
	}

	// 요일을 반환
	public static String getDayOfWeek(int dayOfWeek) {
		String planDayofweek = "";
		switch (dayOfWeek) {
		case 1:
			planDayofweek = "일";
			break;
		case 2:
			planDayofweek = "월";
			break;
		case 3:
			planDayofweek = "화";
			break;
		case 4:
			planDayofweek = "수";
			break;
		case 5:
			planDayofweek = "목";
			break;
		case 6:
			planDayofweek = "금";
			break;
		case 7:
			planDayofweek = "토";
			break;
		}
		return planDayofweek;
	}
	
	public static void main(String[] args) {
		String a = "안녕하세요";
		String[] Arr = a.split("\\?\\?");
		System.out.println(Arr.length);
		//System.out.println(Arr[0]);
		//System.out.println(Arr[1]);
	}

}