package com.ct.services;

import java.util.Random;
import java.util.UUID;

import org.apache.tomcat.util.codec.binary.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ct.dao.UserDAO;
import com.ct.mail.Mail;
import com.ct.model.User;
import com.ct.repositories.IUserDetailsRepository;
import com.ct.repositories.IUserRepository;
import com.ct.security.AuthHelper;

@Service
public class UserService {
	
	@Autowired
	private IUserRepository userRepo;
	
	@Autowired
	private IUserDetailsRepository userDetailsRepo;
	
	@Autowired
	private AuthHelper authHelper;
	
	Mail mail= new Mail();
	
	private Integer generateId(){
		Random r = new Random();
		return r.nextInt(900) + 100; 
	}
	
	public User createUser(UserDAO newUserDAO) {
		System.out.println("In backend create user service method!!");
		UserDAO userDAO = new UserDAO();
		User user=new User();
		StringBuilder str= new StringBuilder();
		str.append(newUserDAO.getFirstName().substring(0,2));
		str.append(newUserDAO.getLastName().substring(0,2));
		Integer id = generateId();
		String userId=str.toString()+id;
		while(userDetailsRepo.exists(userId)){
			id = generateId();
			userId=userId.substring(0, userId.length()-3);
			userId=userId+id;
			
		}
		userDAO.setId(userId);
		userDAO.setFirstName(newUserDAO.getFirstName());
		userDAO.setLastName(newUserDAO.getLastName());
		DateTime dt = new DateTime(DateTimeZone.UTC);		
		userDAO.created_at =  dt.toString(ISODateTimeFormat.dateTime().withZoneUTC());
		userDAO.setPassword(newUserDAO.getPassword()); 
		userDAO.setEmail(newUserDAO.getEmail());
		if(userDetailsRepo.save(userDAO)!=null){			
			user.setId(userDAO.getId());
			user.setEmail(userDAO.getEmail());
			user.setFirstName(userDAO.getFirstName());
			user.setLastName(userDAO.getLastName());
			//user.setPassword(userDAO.getPassword());
			user.setToken(null);
			
		}
		mail.sendEmail(user.getEmail(), user.getId());
		return user;
		
		
	}

	public boolean isValid(String userId) {
		if(!(userDetailsRepo.exists(userId)))
			return false;
		return true;
	}

	public UserDAO getUserDetails(String userId) {
		UserDAO user=new UserDAO();
		if(isValid(userId)){
			user=userDetailsRepo.findOne(userId);			
		}			
		return user;
	}

	private String createAuthToken() {
		String token = Base64.encodeBase64String(UUID.randomUUID().toString()
				.getBytes());
		authHelper.saveToken(token);
		return token;
	}

	public User getAuthenticatedUser() {
		System.out.println("In backend login user service method!!");
		String userId = authHelper.getUsername();
		System.out.println("username from auth helper: "+userId);
		String token = createAuthToken();
		UserDAO userDAO=userDetailsRepo.findById(userId);
		if(userDAO!=null){
			User user=new User();
			user.setId(userDAO.getId());
			user.setEmail(userDAO.getEmail());
			user.setFirstName(userDAO.getFirstName());
			user.setLastName(userDAO.getLastName());
			user.setToken(token);
			return user;
		}
		else{
			System.out.println("Unable to retrieve user from DB!!");
			return null;
		}
			
		
		

	}

}
