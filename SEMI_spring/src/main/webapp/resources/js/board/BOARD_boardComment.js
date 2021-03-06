// 전역변수 : 요청할 다음페이지
let currentPage = document.getElementById("currentPage").value;

/* 스크롤 페이징 */
window.onscroll = function () {
  // 현재 보이는 창 높이
  let window_innerHeight = window.innerHeight;
  // 스크롤 위치
  let nowScroll = window.scrollY || document.documentElement.scrollTop;
  // 보여지는 html body 높이
  let fullHeight = document.body.scrollHeight;

  // 스크롤이 가장 바닦에 다았을때
  if (window_innerHeight + nowScroll >= fullHeight) {
    let boardNo = document.getElementById("boardNo").value;
    console.log(window_innerHeight + nowScroll);
    console.log(fullHeight);

    currentPage++;
    document.getElementById("currentPage").value = currentPage;

    // boardNo = Number(boardNo) or board *= 1
    // 비동기로 서버에 요청시 사용할 데이터
    let jsonObject = { boardNo: boardNo, currentPage: currentPage };

    const xhr = new Xhr("comment/loadCommentList.do", "POST", "json", "application/json; charset=utf-8", jsonObject);
    xhr.async_POST(loadCommentList)
    
    // promise를 사용하여 댓글 생성
    //xhr.async_POST((response) => loadCommentList_Promise(response));
    
  }
};



// Xhr클래스의 비동기 요청 콜백에 사용된 함수 : load commentList to json
function loadCommentList(response) {
	console.log(response.commentList);
	let commentList = response.commentList;

	if (response.commentList.length === 0) {
		console.log("없음");
	    return false;
	} else {
	    for (let i = 0; i < commentList.length; i++) {
	    	// 그룹번호가 0이면 댓글 그 이상이면 대댓글
	    	if (commentList[0].commentGroupSeq <= 0) {
	    		// 함수 : 댓글 생성
	    		let div_comment = makeComment(commentList[i]);
	    		// 함수: 댓글 0이면 '답글 보기' 숨기기
	    		div_comment = noneRecomment(div_comment);
	    		// 자신글이 아니면 삭제 수정 버튼 숨기기
	    		my_control(div_comment);
	    		// 날짜 포멧 변환 및 작성일과 수정일이 다르면 수정일로 표시
	    		format_time(div_comment);
	    	} else {
	    		// 함수 : 대댓글 생성
	    		makeReComment(commentList[i]);
	    		
	    	}
	    }
	}
}

  
// [UI] 댓글 0이면 답글 보기 숨기기
function noneRecomment(div_comment) {
	let commentNo = div_comment.getElementsByClassName("commentNo")[0].value;
	let new_div_comment = document.getElementsByClassName("div_comment");
	
	for(let i = 0; i<new_div_comment.length; i++){
		if(commentNo == new_div_comment[i].getElementsByClassName("commentNo")[0].value){
			let reCommentCount = new_div_comment[i].getElementsByClassName("reCommentCount")[0];
			let comment_bottom = new_div_comment[i].getElementsByClassName("comment_bottom")[0];
			
			if (reCommentCount.value == "0") {
				comment_bottom.classList.add("hidden");
				// comment_bottom.style.display = "none";
				break;
			}
		}
	}
	return div_comment;
}

// [UI] 자신글 이면 삭제, 수정 추가
function my_control(div_comment) {
	let commentNo = div_comment.getElementsByClassName("commentNo")[0].value;
	let new_div_comment = document.getElementsByClassName("div_comment");
	for(let i = 0; i<new_div_comment.length; i++){
		if(commentNo == new_div_comment[i].getElementsByClassName("commentNo")[0].value){
			let myId = document.getElementById("myId");
			let commentId = new_div_comment[i].getElementsByClassName("commentId")[0];
			let comment_mid = new_div_comment[i].getElementsByClassName("comment_mid")[0];
			
			if (myId.value != commentId.value) {
		  		comment_mid.classList.toggle("hidden");
		  		break;
		    }
		}
	}
	return div_comment;  	
}

// [UI] 작성일과 수정일 일치하지않으면 수정일 붙이고 수정일로 변경
function format_time(div_comment) {
	let commentNo = div_comment.getElementsByClassName("commentNo")[0].value;
	let new_div_comment = document.getElementsByClassName("div_comment");
	for(let i = 0; i<new_div_comment.length; i++){
		if(commentNo == new_div_comment[i].getElementsByClassName("commentNo")[0].value){
			const insert_time = new_div_comment[i].getElementsByClassName("insert_time")[0];
			const update_time = new_div_comment[i].getElementsByClassName("update_time")[0];
			const time = new_div_comment[i].getElementsByClassName("time")[0];
			
			if (insert_time.innerHTML == update_time.innerHTML) {
				let date = new Date(Number(insert_time.innerHTML)); // 타입스텝프를
																	// 이용해 js의
																	// 날짜 객체 변환
				time.innerHTML = "작성일 : " + getFormatDate(date);
			} else {
				let date = new Date(Number(update_time.innerHTML));
				time.innerHTML = "수정일 : " + getFormatDate(date);
			}
		}
	}
	return div_comment;
}


// [UI] 본문 글자 일정글자수 보다 크면 자세히보기 만들기
function commnet_contentCut() {
  // let comment_content =

  `<a class="comment_content_aTag">자세히 보기</a>`;
}

// [UI] 대댓글 더보기 클릭  


/* [UI] 대댓글 보이기 이벤트 */
function show_ReComment() {
  // 이벤트를 발생시킨 주체 => a태그
  let target = event.target;
  // 필요한 요소가 전부 들어있는 div
  let comment_body = document.querySelector("#comment_body");
  // 필요한 요소
  let comment_aTag = comment_body.querySelectorAll(".comment_aTag");
  let comment_aTag_close = comment_body.querySelectorAll(".comment_aTag_close");
  let div_reComment = comment_body.querySelectorAll(".div_reComment");

  // 값조회에 필요한 현재 글 번호, 현재 댓글그룹번호
  let boardNo = document.getElementById("boardNo").value;
  let commentGroupNo = comment_body.querySelectorAll(".commentGroupNo");

  for (i in comment_aTag) {
    // 현재 이벤트 발생시킨 a태그의 번지 찾기
    if (target === comment_aTag[i] || target === comment_aTag_close[i]) {
      // comment_aTag(대댓글 개수) <-> comment_aTag_close(답글 숨기기) >> 클릭에 따른 view 변경
      comment_aTag[i].classList.toggle("hidden");
      comment_aTag_close[i].classList.toggle("hidden");

      // 비동기로 서버에 요청시 사용할 데이터 : 현재 부모 글 번호, 요청할 페이지(1), 현재 댓글그룹번호
      let jsonObject = { boardNo: boardNo, reCommentCurrentPage: 1, commentGroupNo: commentGroupNo[i].value };
      console.log("대댓글 요청시 사용 데이터 : " + JSON.stringify(jsonObject));

      if (div_reComment[i].querySelectorAll(".row")[0] === undefined) {
    	  // 비동기 요청에 필요 데이터 설정
    	  const xhr = new Xhr("comment/loadReCommentList.do", "POST", "json", "application/json; charset=utf-8", jsonObject);
    	  // 요청
    	  xhr.async_POST((res) => loadCommentList(res));
      }

      // css를 이용하여 대댓글 보기기
      div_reComment[i].classList.toggle("show");
      break;
    }
  }
}

// [UI] 댓글 생성
function makeComment(commentDto) {
	// 생성 위치
	const commentBody = document.getElementById("comment_body");
	
	const commentGroupDiv = document.createElement("div");
	commentGroupDiv.setAttribute("class", "commentGroup"); // commentDto.commentGroupNo
	
	const commentGroupNo = document.createElement("input");
	commentGroupNo.setAttribute("type", "hidden");
	commentGroupNo.setAttribute("class", "commentGroupNo");
	commentGroupNo.setAttribute("value", commentDto.commentGroupNo);
	commentGroupDiv.appendChild(commentGroupNo);
	
	const row = document.createElement("div");
	row.setAttribute("class","row");
	const div_comment = document.createElement("div");
	div_comment.setAttribute("class", "div_comment");
	
	//<input type="hidden" class="commentGroupNo" name="commentGroupNo" value="${commentDto.commentGroupNo}"/>
	div_comment.innerHTML += `
					<input type="hidden" class="commentId" name="commentId" value="${commentDto.id}"/>
					<input type="hidden" class="commentNo" name="commentNo" value="${commentDto.commentNo}"/>
				 	<div class="row">
						<div class="col-md-1">
							<div class="profile_top">
								<div class="profile_mid">
									<img alt=""	src="${commentDto.profileImg}" onerror="this.src='/update/resources/img/user.png';">
								</div>
							</div>
						</div>
						<div class="col-md-11">
							<div class ="comment_top">
								<span class="writer">${commentDto.commentName}&nbsp;&nbsp;&nbsp;</span><span class="time"></span>
								<span class="insert_time hidden">${commentDto.commentGegdate}</span><span class="update_time hidden">${commentDto.commentUpdateRegDate}</span>
							</div>
							<div class ="comment_content">
								${commentDto.commentContent}<br>
								<a class="comment_content_aTag">자세히 보기</a>
							</div>
							<div class ="comment_mid">추천, <a>답글</a> <span class="my_control"><a>수정</a>&nbsp / &nbsp;<a javascript:; onclick="deleteComment('${commentDto.id}','${commentDto.boardNo}','${commentDto.commentGroupNo}', '${commentDto.commentNo}' );">삭제</a><span></div>
							<div class ="comment_bottom">
								<a class="comment_aTag" onclick="show_ReComment()">답글 ${commentDto.reCommentCount}개 보기</a>
								<input type="hidden" class="reCommentCount" value="${commentDto.reCommentCount}"/>
								<a class="comment_aTag_close hidden" onclick="show_ReComment()">답글 숨기기</a>
							</div>
							
						</div>
					</div>`;
	
	row.appendChild(div_comment);
	commentGroupDiv.appendChild(row)
	
	commentGroupDiv.innerHTML += `<div class="div_reComment"></div> `

  	commentBody.appendChild(commentGroupDiv);
  
	return div_comment;  
}

// [UI] 대댓글 생성
function makeReComment(commentDto) {
  // 자신이 그려져야 하는 위치? >> 그룹번호 >> commentDto안에 있음
  let comment_body = document.querySelector("#comment_body");
  let commentGroupNo_List = comment_body.querySelectorAll(".commentGroupNo");
  let div_reComment_List = comment_body.querySelectorAll(".div_reComment");

  for (i in commentGroupNo_List) {
    if (commentDto.commentGroupNo == commentGroupNo_List[i].value) {
      div_reComment_List[i].innerHTML += `
				<div class="row">
					<input type="hidden" class="commentId" name="commentId" value="${commentDto.id}"/>
					<input type="hidden" class="commentNo" name="commentNo" value="${commentDto.commentNo}"/>
					<div class="col-md-1">
						<div class="profile_top">
							<div class="profile_mid">
								<img alt=""	src="">
							</div>
						</div>
					</div>
							
					<div class="col-md-11">
						<div class="col-md-1">
							<div class="profile_top">
								<div class="profile_mid">
									<img alt=""	src="${commentDto.profileImg}" onerror="this.src='/update/resources/img/user.png';">
								</div>
							</div>
						</div>
						<div class="col-md-11">
							<div class ="comment_top">
								<span class="writer">${commentDto.commentName}&nbsp;&nbsp;&nbsp;</span><span class="time"></span>
								<span class="insert_time hidden">${commentDto.commentGegdate}</span><span class="update_time hidden">${commentDto.commentUpdateRegDate}</span>
							</div>
							<div class ="reComment_content">
								${commentDto.commentContent}<br>
								<a class="comment_content_aTag">자세히 보기</a>
							</div>
							<div class ="comment_mid">추천, <a>답글</a></div>
						</div>
					</div>
				 </div> `;
      break;
    }
  }
}

// [UI] 작성 글자수 카운트
function countWord() {
  let commentContent = document.querySelector("#commentContent").innerHTML;
  let countWord = document.querySelector("#countWord");
  let maxLength = 500;

  // 태그제거한 글자 개수
  let newText = commentContent.replace(/(<([^>]+)>)/gi, "");
  let textLength = newText.length;

  // <br>의 크기를 2로 잡고 증가
  if (commentContent.indexOf("<br>") != -1) {
    // 결과 없으면 -1 리턴
    // <br>의 총개수 *2
    textLength += (commentContent.match(/<br>/g) || []).length * 2;
  }

  if (maxLength - textLength <= 0) {
    countWord.style.color = "red";
    countWord.innerHTML = textLength + "/500 최대 글자수를 초과했습니다.";
  } else {
    countWord.style.color = "black";
    countWord.innerHTML = textLength + "/500";
  }
  return false;
}

// 댓글 입력
function inputComment() {
  let commentContent = document.querySelector("#commentContent");
  let boardNo = document.getElementById("boardNo").value;

  if (commentContent.innerHTML !== "") {
    let maxLength = 500;
    let newText = commentContent.innerHTML.replace(/(<([^>]+)>)/gi, "");
    let textLength = newText.length;

    if (maxLength - textLength <= 0) {
      alert("작성가능한 최대 글자수를 초과했습니다.");
      return false;
    }
    // 댓글 작성 부모글, id(서버에서 세션), 작성자(서버에서 세션), 내용
    let jsonObject = { boardNo: boardNo, commentContent: commentContent.innerHTML };

    // 서버 전송
    let xhr = new Xhr("comment/inputComment.do", "POST", "", "", jsonObject);
    xhr.async_POST(loadCommentDto);

    commentContent.innerHTML = "";
  } else {
    alert("값을 입력하세요");
    return false;
  }
}


// 뎃글 삭제 : 아이디(서버에서 작성자 일치 세션 확인), 댓글테이블pk > 하위 속한 대댓글 모두 삭제
function deleteComment(commentId, boardNo, commentGroupNo, commentNo){
	let deleteConfirm = confirm("댓글을 댓글에 포함된 대댓글도 모두 삭제됩니다. 삭제하시겠습니까?");
	if(deleteConfirm == false ){return false;} 		// 취소 클릭시
	
	const myId = document.getElementById("myId")
	if(myId == undefined){return false;} 			// 로그인 되있지 않으면
	
	// 비동기 통신준비 : 작성자id, 부모글 번호, 그룹번호 => 댓글은 하위 대댓글 까지 모두 삭제 시킨다. 
	const json = {id: commentId, boardNo: boardNo, commentGroupNo: commentGroupNo}
	const xhr = new Xhr("comment/commentDelete.do" ,"post", "", "", json);
	
	// 비동기 실행(callback)
	xhr.async_POST((response) => {
		if (response.res === "success") {
			const getTagArr = document.getElementsByClassName("commentNo");
			for(let tag of getTagArr){
				if(tag.value == commentNo){
					let target_div = tag.parentNode.parentNode.parentNode;
					// target_row의 부모Node로 갔다가 다시 그 부모의 자식인 target_row을 삭제
					target_div.parentNode.removeChild(target_div); 
					alert("댓글이 삭제되었습니다.");
					return;
				}
			}
		// 서버에서 유효성검사 실행
		} else if(response.res === "Noid"){
			alert("작성자만 삭제할 수 있습니다.")
		}
	});
}


//[사용안함]  글작성 비동기 콜백
function loadCommentDto(response) {
  console.log(response);
  if (response.res === "success") {
    // (response.commentDto)
  }
}

//[사용안함] 상위에 작성한 글 생성
function makeComment_insert(commentDto) {
  const commentBody = document.getElementById("comment_body");
  let main_row = document.createElement("div");
  main_row.setAttribute("class", "row");

  let commentGroupNo = document.createElement("input");
  commentGroupNo.setAttribute("type", "hidden");
  commentGroupNo.setAttribute("class", "commentGroupNo");
  commentGroupNo.setAttribute("name", "commentGroupNo");
  commentGroupNo.value;
}





/* =============================================== 댓글 그리기 : Promise를 사용한 예시 ======================================================*/ 


function loadCommentList_Promise(response){
	console.log("Promise를 사용한 댓글 대댓글 로드")
	let commentList = response.commentList;
	console.log(commentList)
	if (commentList.length === 0) {
		console.log("결과없음");
		return;
    } else {
    	for(let i = 0; i<commentList.length; i++){
    		// 댓글
    		if (commentList[0].commentGroupSeq <= 0) {
    			// 댓글 그리기
	    		load_makeComment(commentList[i])
	    		.then((div_comment) => load_noneRecomment(div_comment)) 	// == .then(load_noneRecomment);
	    		.catch(console.log)											// == .catch((error) => console.log(error));
	    		.then(load_my_control)
	    		.catch(console.log)
	    		.then(load_format_time)
	    		.catch(console.log);
    		// 대댓글
    		} else {
    			// 대댓글 그리기
	    		makeReComment(commentList[i]);
	    		if (i == commentList.length - 1) {
	    		}
    		}
    	}
    }
}

const load_makeComment = (commentDto) => {
	// 댓글 그리기
	let div_comment = makeComment(commentDto);
	// 리턴 Promise
	return new Promise((resolve, reject) => {
		// res true 결과로 리턴
		if(div_comment){
			resolve(div_comment);
		} else {
			reject(new Error("2. 결과 없음"));
		}
	})
}

const load_noneRecomment = (div_comment) => {
	let resDom = noneRecomment(div_comment);
	return new Promise((resolve, reject) => {
		if(div_comment){
			resolve(div_comment);
		} else {
			reject(new Error("3. 결과 없음"));
		}
	});
}

const load_my_control = (div_comment) => {
	let resDom = my_control(div_comment);
	return new Promise((resolve, reject) => {
		if(div_comment){
			resolve(div_comment);
		} else {
			reject(new Error("4. 결과 없음"));
		}
	});
}

const load_format_time = (div_comment) =>{
	let resDom = format_time(div_comment);
	return new Promise((resolve, reject) => {
		if(div_comment){
			resolve(div_comment);
		} else {
			reject(new Error("5. 결과 없음"));
		}
	});
}

