package global.sesoc.Project_3jo.controller;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import global.sesoc.Project_3jo.dao.QnaDAO;
import global.sesoc.Project_3jo.util.FileService;
import global.sesoc.Project_3jo.vo.PageNavigator;
import global.sesoc.Project_3jo.vo.FaqVO;
import global.sesoc.Project_3jo.vo.QnaVO;
import global.sesoc.Project_3jo.vo.SearchVO;




@Controller
@RequestMapping ("qna")
public class QnaController {
   private static final Logger logger = LoggerFactory.getLogger(QnaController.class);
   
   @Autowired
   QnaDAO dao;
   
   // 게시판 관련 상수값들
   final int countPerPage = 5; // 페이지당 글 수
   final int pagePerGroup = 5; // 페이지 이동 링크를 표시할 페이지 수
   final String uploadPath = "/qnafile"; // 파일 업로드 경로
   
   /**
    * 1:1문의 목록 보기
    */
     @RequestMapping (value="main", method=RequestMethod.GET)
     public String qnaList(@RequestParam(value="page", defaultValue="1") int page
     , SearchVO search, Model model, HttpSession session) {
     
    	 String po_id = (String) session.getAttribute("po_id");
			
    	 //본인 글인지 확인할 로그인아이디
    	 search.setPo_id(po_id);
		  
        logger.info("page: {}, search: {}", page, search);
        
        int total = dao.getTotal(search); //전체 글 개수
        logger.info("total: {}", total);
        
        //페이지 계산을 위한 객체 생성
        PageNavigator navi = new PageNavigator(countPerPage, pagePerGroup, page, total);
        
        //검색어와 시작 위치, 페이지당 글 수를 전달하여 목록 읽기
        ArrayList<QnaVO> qnalist = dao.listQna(search, navi.getStartRecord(), navi.getCountPerPage());
        
        //페이지 정보 객체와 글 목록, 검색어를 모델에 저장
        model.addAttribute("qnalist", qnalist);
        model.addAttribute("navi", navi);
        model.addAttribute("search", search);
        
        model.addAttribute("type", search.getType());
        
        logger.debug("qnaList: {}", qnalist);
        return "qnajsp/qnaList";
     }
     
   /**
    * 1:1문의 글 읽기 
    * @param q_num 읽을 글번호
    * @return 해당 글 정보
    */
   @RequestMapping(value = "read", method = RequestMethod.GET)
   public String qnaRead(@RequestParam(value="page", defaultValue="1") int page
           , SearchVO search, int q_num, Model model, HttpSession session) {
	   
	  String po_id = (String) session.getAttribute("po_id");
  	  search.setPo_id(po_id);
      
      // 전달된 글 번호로 해당 글정보 읽기
      QnaVO qna = dao.getQna(q_num);
      if (qna == null) {
         return "redirect:qna";
      }
      
      // 본문글정보를 모델에 저장
      model.addAttribute("qna", qna);
              
      //qnaList 불러오기
      int total = dao.getTotal(search);
      PageNavigator navi = new PageNavigator(countPerPage, pagePerGroup, page, total);
      
      logger.info("page: {}, search: {}", page, search);
      logger.debug("total: {}", total);
      
      
      ArrayList<QnaVO> qnalist = dao.listQna(search, navi.getStartRecord(), navi.getCountPerPage());
      logger.debug("첨부파일 확인 컨트롤러{}", qna);
      model.addAttribute("qnalist", qnalist);
      model.addAttribute("navi", navi);
      model.addAttribute("search", search);
      
      return "qnajsp/qnaRead";
   }
   
   /**
    * 문의글 작성 폼 보기
    */
   @RequestMapping (value="write", method=RequestMethod.GET)
   public String write() {
      return "qnajsp/qnaWrite";
   }
   
   /** 
    * 문의글 저장
    */
   @RequestMapping (value="write", method=RequestMethod.POST)
   public String write(
         HttpSession session
         , Model model
         , QnaVO qna
			/* , String list_cg */
         , MultipartFile upload) {
      
      //세션에서 로그인한 사용자의 아이디를 읽어서 Board객체의 작성자 정보에 세팅
      String po_id = (String) session.getAttribute("po_id");
      qna.setPo_id(po_id);
      logger.debug("저장할 글 정보 : {}", qna);
      
      /* faq 리스트 불러올 곳 dao.list; */
		/*
		 * ArrayList<FaqVO> callfaq = dao.callFaq(list_cg);
		 * model.addAttribute("callfaq", callfaq); model.addAttribute("list_cg",
		 * list_cg); logger.debug("셀렉트 박스 값 받아오는지 : {}", list_cg);
		 * logger.debug("셀렉트 박스 값 포함한 FaqVO 객체인 callfaq 받아오는지 : {}", callfaq);
		 */
      
      
      //첨부파일이 있는 경우 지정된 경로에 저장하고, 원본 파일명과 저장된 파일명을 Board객체에 세팅
      if (!upload.isEmpty()) {
         String q_savedfile = FileService.saveFile(upload, uploadPath);
         qna.setQ_originalfile(upload.getOriginalFilename());
         qna.setQ_savedfile(q_savedfile);
      }
      
      dao.insertQna(qna);
      return "redirect:main";
   }
   
   /**
	 * faq 목록 가져오기
	 */
	@ResponseBody
	@RequestMapping (value="callfaq", method=RequestMethod.GET)
	public ArrayList<FaqVO> callfaq(String select_cg) {
		ArrayList<FaqVO> callFaq = dao.callfaq(select_cg);
		logger.debug("라디오 버튼 값~~~~~~~~~~{}", select_cg);
		logger.debug("들어오는 FaqVO 객체 배열 값 ~~~~~~~~~~~~~~~~{}", callFaq);
		return callFaq;
	}
	
   /**
    * 파일 다운로드 (파일이 첨부된 글 번호 q_num)
    */
   @RequestMapping(value = "showfile", method = RequestMethod.GET)
   public String fileDownload(int q_num, Model model, HttpServletResponse response) {
      QnaVO qna = dao.getQna(q_num);
      logger.info("쇼파일에 글 번호 {}", q_num);
      logger.info("쇼파일에 2{}", qna);
      
      //원래의 파일명
      String q_originalfile = new String(qna.getQ_originalfile());
      try {
         response.setHeader("Content-Disposition", " attachment;filename="+ URLEncoder.encode(q_originalfile, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
      }
      
      //저장된 파일 경로
      String fullPath = uploadPath + "/" + qna.getQ_savedfile();
      logger.info("쇼파일에 1{}", fullPath);
      
      //서버의 파일을 읽을 입력 스트림과 클라이언트에게 전달할 출력스트림
      FileInputStream filein = null;
      ServletOutputStream fileout = null;
      
      try {
         filein = new FileInputStream(fullPath);
         fileout = response.getOutputStream();
         
         //Spring의 파일 관련 유틸 이용하여 출력
         FileCopyUtils.copy(filein, fileout);
         
         filein.close();
         fileout.close();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return null;
   }
   
   /**
    * 글 삭제
    */
   @RequestMapping (value="delete", method=RequestMethod.POST)
   public String delete(HttpSession session, int[] q_nums) {
      logger.info("{}", q_nums.length);
      
	  //삭제할 글 번호와 본인 글인지 확인할 로그인아이디
      String po_id = (String) session.getAttribute("po_id");
      QnaVO qna = null;
      String q_savedfile = null;
      int result = 0;
      
	  for (int n : q_nums) {
		  //첨부된 파일이 있는지 먼저 확인
		  qna = dao.getQna(n);
		  q_savedfile = qna.getQ_savedfile();
	  
		  //글 삭제
		  result = dao.deleteQna(qna);
	  
		  //글 삭제 성공 and 첨부된 파일이 있는 경우 파일도 삭제
		  if (result == 1 && q_savedfile != null) {
			  FileService.deleteFile(uploadPath + "/" + q_savedfile); 
		  }
	  }       
      return "redirect:main";
   }
   
   
}