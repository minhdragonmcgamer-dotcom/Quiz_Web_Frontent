package com.example.quiz.controller;

import com.example.quiz.entity.*;
import com.example.quiz.repository.OptionAnswerRepository;
import com.example.quiz.repository.QuizRepository;
import com.example.quiz.repository.ResultRepository;

import com.example.quiz.repository.QuestionRepository;

import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.*;

/* 
admin
giao dien
import cau hoi excel
rang buoc
bao cao
*/
@Controller
public class QuizController {

    @Autowired
    private QuizRepository quizRepo;

    @Autowired
    private OptionAnswerRepository optionRepo;

    @Autowired
    private ResultRepository resultRepo;

    @Autowired
    private QuestionRepository questionRepo;
    
    //HÀM CHUNG CHO TẠO VÀ SỬA QUIZ
    private void buildQuizFromParams(Quiz quiz, Map<String, String> params) {

        quiz.setTitle(params.get("title"));

        List<Question> questions = new ArrayList<>();


        for (int i = 1; i <= 100; i++) {

            String content = params.get("q_" + i);
            if (content == null || content.isBlank()) continue;

            Question q = new Question();
            q.setContent(content);
            q.setQuiz(quiz);

            List<OptionAnswer> options = new ArrayList<>();

            int correct = Integer.parseInt(params.get("q_" + i + "_correct"));

            for (int j = 1; j <= 4; j++) {
                String optContent = params.get("q_" + i + "_opt" + j);

                if (optContent != null && !optContent.isBlank()) {
                    OptionAnswer opt = new OptionAnswer();
                    opt.setContent(optContent);
                    opt.setCorrect(j == correct);
                    opt.setQuestion(q);
                    options.add(opt);
                }
            }

            q.setOptions(options);
            questions.add(q);
        }

        quiz.getQuestions().clear();
        quiz.getQuestions().addAll(questions);
    }


    //TẠO QUIZ
    @GetMapping("/teacher/create")
    public String createPage(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null || !"ROLE_TEACHER".equals(user.getRole())) {
            return "redirect:/login";
        }

        model.addAttribute("quiz", new Quiz());
        model.addAttribute("action", "/teacher/create");

        return "quiz_form";
    }

    @PostMapping("/teacher/create")
    public String createQuiz(@RequestParam Map<String, String> params,
                            HttpSession session) {

        User user = (User) session.getAttribute("user");

        Quiz quiz = new Quiz();
        quiz.setUser(user);

        buildQuizFromParams(quiz, params);

        quizRepo.save(quiz);

        return "redirect:/quizzes";
    }
    

    //SỬA QUIZ 
    @GetMapping("/teacher/edit/{id}")
    public String editPage(@PathVariable Long id,
                        HttpSession session,
                        Model model) {

        User user = (User) session.getAttribute("user");

        Quiz quiz = quizRepo.findById(id).orElse(null);

        if (quiz == null || !quiz.getUser().getId().equals(user.getId())) {
            return "redirect:/quizzes";
        }

        model.addAttribute("quiz", quiz);
        model.addAttribute("action", "/teacher/edit/" + id);
        return "quiz_form";
    }

    @PostMapping("/teacher/edit/{id}")
    public String updateQuiz(@PathVariable Long id,
                            @RequestParam Map<String, String> params,
                            HttpSession session) {

        User user = (User) session.getAttribute("user");

        Quiz quiz = quizRepo.findById(id).orElse(null);

        if (quiz == null || !quiz.getUser().getId().equals(user.getId())) {
            return "redirect:/quizzes";
        }

        quiz.getQuestions().clear();

        buildQuizFromParams(quiz, params);

        quizRepo.save(quiz);

        return "redirect:/quizzes";
    }

    //IMPORT QUIZ TỪ EXCEL
    @PostMapping("/teacher/import")
    public String importNewQuiz(@RequestParam("title") String title,
                            @RequestParam("file") MultipartFile file,
                            HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) return "redirect:/login";

        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setUser(user);

        List<Question> questions = new ArrayList<>();

        try {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String content = row.getCell(0).toString().trim();
                if (content.isEmpty()) continue;

                Question q = new Question();
                q.setContent(content);
                q.setQuiz(quiz);

                List<OptionAnswer> options = new ArrayList<>();

                Cell correctCell = row.getCell(5);
                if (correctCell == null) continue;

                int correct;
                try {
                    correct = (int) correctCell.getNumericCellValue();
                } catch (Exception e) {
                    try {
                        correct = Integer.parseInt(correctCell.toString().replace(".0", ""));
                    } catch (Exception ex) {
                        continue;
                    }
                }

                for (int j = 1; j <= 4; j++) {
                    String optContent = row.getCell(j).toString();

                    OptionAnswer opt = new OptionAnswer();
                    opt.setContent(optContent);
                    opt.setCorrect(j == correct);
                    opt.setQuestion(q);

                    options.add(opt);
                }

                q.setOptions(options);
                questions.add(q);
            }

            quiz.setQuestions(questions);

            System.out.println("Imported questions: " + questions.size());

            quizRepo.save(quiz);
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/quizzes";
    }   

    //XÓA CÂU HỎI
    @PostMapping("/teacher/question/delete/{id}")
    public String deleteQuestion(@PathVariable Long id,
                                @RequestParam Long quizId) {

        questionRepo.deleteById(id);

        return "redirect:/teacher/edit/" + quizId;
    }

    //DANH SÁCH QUIZ
    @GetMapping("/quizzes")
    public String listQuiz(Model model) {
        model.addAttribute("quizzes", quizRepo.findAll());
        return "quiz_list";
    }

    //LÀM QUIZ
    @GetMapping("/quiz/{id}")
    public String takeQuiz(@PathVariable Long id, Model model) {

        Quiz quiz = quizRepo.findById(id).orElse(null);

        model.addAttribute("quiz", quiz);
        return "quiz_take";
    }


    //XÓA QUIZ
    @Transactional 
    @GetMapping("/teacher/delete/{id}")
    public String deleteQuiz(@PathVariable Long id, HttpSession session) {

        User user = (User) session.getAttribute("user");

        Quiz quiz = quizRepo.findById(id).orElse(null);

        if (quiz == null || user == null || !quiz.getUser().getId().equals(user.getId())) {
            return "redirect:/quizzes";
        }

        // xóa kết quả liên quan
        resultRepo.deleteByQuiz_QuizId(id);
        quizRepo.delete(quiz);

        return "redirect:/quizzes";
    }

    //NỘP BÀI
    @PostMapping("/submit")
    public String submit(@RequestParam Map<String, String> answers,
                         HttpSession session,
                         Model model) {
        
        Long quizId = Long.parseLong(answers.get("quizId"));
        Quiz quiz = quizRepo.findById(quizId).orElse(null);

        int score = 0;
        int total = quiz.getQuestions().size();

        for (String key : answers.keySet()) {

            if (key.startsWith("q_")) {

                Long optionId = Long.parseLong(answers.get(key));
                OptionAnswer opt = optionRepo.findById(optionId).orElse(null);

                if (opt != null && opt.isCorrect()) {
                    score++;
                }
            }
        }

        User user = (User) session.getAttribute("user");

        Result result = new Result();
        result.setUser(user);
        result.setScore(score);
        result.setTotalQuestion(total);
        

        result.setQuiz(quiz);

        resultRepo.save(result);

        model.addAttribute("score", score);
        model.addAttribute("total", total);

        
        return "result";
    }


}