package com.ct.services;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ct.algorithms.Report;
import com.ct.algorithms.Vote;
import com.ct.controllers.PostController;
import com.ct.dao.PostDAO;
import com.ct.dao.PostUserDAO;
import com.ct.model.Post;
import com.ct.model.PostUser;
import com.ct.repositories.IPostRepository;
import com.ct.repositories.IPostUserRepository;

@Service
public class PostService {

	@Autowired
	private IPostRepository postRepo;
	
	@Autowired
	private IPostUserRepository postUserRepo;
	
	private static final Logger LOGGER = Logger.getLogger(PostService.class.getName());
	
	public PostService() {
		super();
	}

	public Post createPost(Post post) {
		PostDAO postDAO = new PostDAO();
		int id = generateId();
		while (postRepo.exists(id))
			id = generateId();
		postDAO=setPostDAOObj(post, postDAO);
		postDAO.setId(id);
		DateTime dt = new DateTime(DateTimeZone.UTC);
		postDAO.setCreatedOn(dt.toString(ISODateTimeFormat.dateTime().withZoneUTC()));
		postDAO.setLastEditedOn(dt.toString(ISODateTimeFormat.dateTime().withZoneUTC()));
		if (postRepo.save(postDAO) != null) {
			LOGGER.info("Post created");
			post.setId(postDAO.getId());
			post.setCreatedOn(postDAO.getCreatedOn());
			post.setLastEditedOn(postDAO.getLastEditedOn());
		}
		return post;
	}

	public Post getPost(int id) {
		Post post = new Post();
		PostDAO postDAO = new PostDAO();
		postDAO = postRepo.findOne(id);
		post = setPostObj(post, postDAO);
		LOGGER.info("Post fetched");
		return post;

	}

	public Post updatePost(Post post) {
		PostDAO postDAO = new PostDAO();
		postDAO = postRepo.findById(post.getId());
		postDAO.setContent(post.getContent());
		postDAO.setHeadline(post.getHeadline());
		DateTime dt = new DateTime(DateTimeZone.UTC);
		postDAO.setLastEditedOn(dt.toString(ISODateTimeFormat.dateTime().withZoneUTC()));
		if(postRepo.save(postDAO)!=null){
			LOGGER.info("Post updated");
			setPostObj(post, postDAO);
			post.setLastEditedOn(postDAO.getLastEditedOn());
		}
		return post;
	}

	public void deletePost(int id) {
		PostDAO postDAO = new PostDAO();
		postDAO = postRepo.findById(id);
		postRepo.delete(postDAO);
		LOGGER.info("Post deleted");
	}

	public ArrayList<Post> getPostsForCategory(String category,String university) {
		ArrayList<Post> posts = new ArrayList<Post>();
		ArrayList<PostDAO> postDAOs = new ArrayList<PostDAO>();
		postDAOs = postRepo.findByCategoryAndUniversityOrderByLastEditedOnDesc(category,university);
		//postDAOs = postRepo.findByCategoryAndUniversity(category,university);
		System.out.println("postDao sizeeee....."+postDAOs.size() );
		//postDAOs = (ArrayList<PostDAO>)postRepo.findAll();
		for (PostDAO postDAO : postDAOs) {
			Post post = new Post();
			post = setPostObj(post, postDAO);
			posts.add(post);
		}
		LOGGER.info("Posts fetched");
		return posts;
	}

	public boolean postExists(int id) {
		if (!(postRepo.exists(id)))
			return false;
		return true;
	}
	
	public boolean userActionExists(String userid) {
		if (postUserRepo.findByUser(userid)==null)
			return false;
		return true;
	}
	
	public Post votePost(int voteType,String user_id,Post post){
		PostDAO postDAO = new PostDAO();
		postDAO=postRepo.findById(post.getId());
		int newVoteScore= Vote.calculateVoteScore(voteType, postDAO.getVoteScore());
		postDAO.setVoteScore(newVoteScore);
		DateTime dt = new DateTime(DateTimeZone.UTC);
		postDAO.setLastEditedOn(dt.toString(ISODateTimeFormat.dateTime().withZoneUTC()));
		if (postRepo.save(postDAO) != null) {
			LOGGER.info("Post voted");
			setPostObj(post, postDAO);
			post.setVoteScore(postDAO.getVoteScore());
			post.setLastEditedOn(postDAO.getLastEditedOn());
			PostUserDAO userActionsDAO=postUserRepo.findByUser(user_id);
			if(userActionsDAO!=null){
				updateUserVoteActions(voteType,post.getId(),userActionsDAO);
				postUserRepo.save(userActionsDAO);
				LOGGER.info("User actions saved");
			}else{
				PostUserDAO userActionsDAO1 = new PostUserDAO();
				userActionsDAO1.setUser(user_id);
				updateUserVoteActions(voteType,post.getId(),userActionsDAO1);
				postUserRepo.save(userActionsDAO1);
				LOGGER.info("User Actions saved");
			}
			
		}
		return post;
	}
	
	public Post reportPost(String user_id,Post post){
		PostDAO postDAO = new PostDAO();
		postDAO=postRepo.findById(post.getId());
		int newReportScore=Report.updateReportScore(postDAO.getReportScore());
		if(Report.removeContent(newReportScore)){
			postRepo.delete(postDAO.getId());
			LOGGER.info("Post deleted");
			return null;
		}else{
			DateTime dt = new DateTime(DateTimeZone.UTC);
			postDAO.setLastEditedOn(dt.toString(ISODateTimeFormat.dateTime().withZoneUTC()));
			postDAO.setReportScore(newReportScore);
			if(postRepo.save(postDAO)!=null){
				LOGGER.info("Post reported");
				setPostObj(post, postDAO);
				post.setReportScore(postDAO.getReportScore());
				post.setLastEditedOn(postDAO.getLastEditedOn());
				PostUserDAO userActionsDAO=postUserRepo.findByUser(user_id);
				if(userActionsDAO!=null){
					userActionsDAO.getReportedPosts().add(post.getId());
					postUserRepo.save(userActionsDAO);
					LOGGER.info("User Actions saved");
				}else{
					PostUserDAO userActionsDAO1 = new PostUserDAO();
					userActionsDAO1.setUser(user_id);
					userActionsDAO1.getReportedPosts().add(post.getId());
					postUserRepo.save(userActionsDAO1);
					LOGGER.info("User actions Saved");
				}
				
			}
			return post;
		}
	}
	
	public void followPost(String user_id,int post_id){
		PostUserDAO userActionsDAO = postUserRepo.findByUser(user_id);
		PostDAO postDAO = postRepo.findById(post_id);
		if (userActionsDAO != null) {
			if(userActionsDAO.getFollowingPosts().contains(post_id)){
				userActionsDAO.getFollowingPosts().remove(Integer.valueOf(post_id));
				postDAO.setFollowCount(postDAO.getFollowCount()-1);
			}else{
				userActionsDAO.getFollowingPosts().add(post_id);
				postDAO.setFollowCount(postDAO.getFollowCount()+1);
			}
	    	postUserRepo.save(userActionsDAO);
			postRepo.save(postDAO);
			LOGGER.info("Post follow updated");
		} else {
			PostUserDAO userActionsDAO1 = new PostUserDAO();
			userActionsDAO1.setUser(user_id);
			userActionsDAO1.getFollowingPosts().add(post_id);
			postDAO.setFollowCount(postDAO.getFollowCount()+1);
			postUserRepo.save(userActionsDAO1);
			postRepo.save(postDAO);
			LOGGER.info("Post follow updated");
		}
	}
	
	public PostUser getUserActioForPosts(String userId){
		PostUserDAO postUserDAO = postUserRepo.findByUser(userId);
		PostUser postUser = new PostUser();
		postUser.setUser(userId);
		postUser.setUpVotedPosts(postUserDAO.getUpVotedPosts());
		postUser.setDownVotedPosts(postUserDAO.getDownVotedPosts());
		postUser.setReportedPosts(postUserDAO.getReportedPosts());
		postUser.setFollowingPosts(postUserDAO.getFollowingPosts());
		postUser.setReportedComments(postUserDAO.getReportedComments());
		LOGGER.info("User actions fetched");
		return postUser;
	}
	
	private void  updateUserVoteActions(int voteType,int postid,PostUserDAO userActionsDAO){
		if(voteType==1){
			LOGGER.info("Post Upvoted");
			userActionsDAO.getUpVotedPosts().add(postid);
		}else if(voteType==2){
			LOGGER.info("Post Upvote removed");
			userActionsDAO.getUpVotedPosts().remove(Integer.valueOf(postid));
		}else if(voteType==3){
			LOGGER.info("Post Downvoted");
			userActionsDAO.getDownVotedPosts().add(postid);
		}else if(voteType==4){
			LOGGER.info("Post Downvote removed");
			userActionsDAO.getDownVotedPosts().remove(Integer.valueOf(postid));
		}
	}

	private Integer generateId() {
		Random r = new Random();
		return r.nextInt(9000) + 1000;
	}
	
	private PostDAO setPostDAOObj(Post post, PostDAO postDAO){
		postDAO.setId(post.getId());
		postDAO.setHeadline(post.getHeadline());
		postDAO.setContent(post.getContent());
		postDAO.setUserId(post.getUserId());
		postDAO.setVoteScore(post.getVoteScore());
		postDAO.setIsAlert(post.getIsAlert());
		postDAO.setCategory(post.getCategory());
		postDAO.setWebLink(post.getWebLink());
		postDAO.setImgURL(post.getImgURL());
		postDAO.setReportScore(post.getReportScore());
		postDAO.setUniversity(post.getUniversity());
		postDAO.setFollowCount(post.getFollowCount());
		postDAO.setCreatedOn(post.getCreatedOn());
		postDAO.setLastEditedOn(post.getLastEditedOn());
		return postDAO;
	}

	private Post setPostObj(Post post, PostDAO postDAO) {
		post.setId(postDAO.getId());
		post.setHeadline(postDAO.getHeadline());
		post.setContent(postDAO.getContent());
		post.setUserId(postDAO.getUserId());
		post.setVoteScore(postDAO.getVoteScore());
		post.setIsAlert(postDAO.getIsAlert());
		post.setCategory(postDAO.getCategory());
		post.setWebLink(postDAO.getWebLink());
		post.setImgURL(postDAO.getImgURL());
		post.setReportScore(postDAO.getReportScore());
		post.setUniversity(postDAO.getUniversity());
		post.setFollowCount(postDAO.getFollowCount());
		post.setCreatedOn(postDAO.getCreatedOn());
		post.setLastEditedOn(postDAO.getLastEditedOn());
		return post;
	}
	
	

}
