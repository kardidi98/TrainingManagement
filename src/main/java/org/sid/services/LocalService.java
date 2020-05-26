package org.sid.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sid.dao.LocalRepository;
import org.sid.entities.Local;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


@Service
public class LocalService {
	@Autowired
	LocalRepository localRepository;
	
	public List<Local> getLocals(){
		List<Local> locals=new ArrayList<Local>();
		localRepository.findAll().forEach(locals::add);
		return locals;
	}
	
	public void addLocal(Local local) {
		localRepository.save(local);
	}
	
	public Page<Local> getPageLocals(int page,int size){
		return localRepository.findAll(PageRequest.of(page, size));
	}
	
	public List<Integer> getPageLocalsNavigation(Page<Local> pageLocals){
		return Arrays.asList(new Integer[pageLocals.getTotalPages()]);
	}
	
	public List<String> getLocalsVilles(){
		return localRepository.getLocalsVilles();
	}
	
	public List<String> getLocalsCategories(){
		return localRepository.getLocalsCategories();
	}

}
