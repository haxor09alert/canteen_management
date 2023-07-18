package com.system.canteen_management.services.user_management.impl;

import com.system.canteen_management.config.PasswordEncoderUtil;
import com.system.canteen_management.dto.AuthenticationResponse;
import com.system.canteen_management.dto.LoginDto;
import com.system.canteen_management.entity.user_management.User;
import com.system.canteen_management.pojo.user_management.UserPojo;
import com.system.canteen_management.repo.user_management.EmailCredRepo;
import com.system.canteen_management.repo.user_management.UserRepo;
import com.system.canteen_management.security.JwtService;
import com.system.canteen_management.services.user_management.UserService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final JavaMailSender getJavaMailSender;
    private final EmailCredRepo emailCredRepo;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Autowired
    @Qualifier("emailConfigBean")
    private Configuration emailConfig;


    private final UserRepo userRepo;
    public static String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/canteen_mgmt";
//
//    UserServiceImpl(UserRepo userRepo) {
//        this.userRepo = userRepo;
//    }

    @Override
    public UserPojo save(UserPojo userPojo) throws IOException {
        User user = new User();
        user.setEmail(userPojo.getEmail());
        user.setFullName(userPojo.getFullname());
        user.setMobileNo(userPojo.getMobile_no());
        user.setPassword(PasswordEncoderUtil.getInstance().encode(userPojo.getPassword()));
        userRepo.save(user);
        return new UserPojo(user);
    }

    public List<User> fetchAll() {
        return userRepo.findAll();
    }

    public User fetchById(Integer id) {
        return userRepo.findById(id).orElseThrow(() -> new RuntimeException("Not Found"));
    }

    public void deleteById(Integer id) {
        userRepo.deleteById(id);
    }

    @Override
    public void sendEmail() {
        try {
            Map<String, String> model = new HashMap<>();

            MimeMessage message = getJavaMailSender.createMimeMessage();
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            Template template = emailConfig.getTemplate("emailTemp.ftl");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

            mimeMessageHelper.setTo("sendfrom@yopmail.com");
            mimeMessageHelper.setText(html, true);
            mimeMessageHelper.setSubject("Registration");
            mimeMessageHelper.setFrom("sendTo@yopmail.com");

            taskExecutor.execute(new Thread() {
                public void run() {
                    getJavaMailSender.send(message);
                }
            });
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    @Override
    public AuthenticationResponse authenticate(LoginDto loginDto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getEmail(), loginDto.getPassword()
                )
        );

        UserDetails userDetails = userRepo.findByEmail(loginDto.getEmail()).orElseThrow(() -> new EntityNotFoundException("User not found."));
        String jwtToken = jwtService.generateToken(userDetails);
        return AuthenticationResponse.builder().token(jwtToken).build();
    }
}
