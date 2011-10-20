/**
 * 
 */
package com.learning.slate.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author cbryant
 *
 */
@XStreamAlias("library")
public class Library {
	
	public static abstract class LibraryEntry {
		
		private List<Piece> pieces;
		
		public List<Piece> getPieces() {
			return pieces;
		}
		public void setPieces(List<Piece> pieces) {
			this.pieces = pieces;
		}
	}

	@XStreamAlias("challenge")
	public static class Challenge extends LibraryEntry {
		
		@XStreamAsAttribute
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
	
	@XStreamAlias("solution")
	public static class Solution extends LibraryEntry {
		
		@XStreamAsAttribute
		private String idref;

		public String getIdref() {
			return idref;
		}
		public void setIdref(String idref) {
			this.idref = idref;
		}
	}
	
	@XStreamAlias("piece")
	public static class Piece {
		
		@XStreamAsAttribute
		private String name;
		@XStreamAsAttribute
		private Float angle;
		@XStreamAsAttribute
		private Float x;
		@XStreamAsAttribute
		private Float y;
		
		public Piece(String name, float angle, float x, float y) {
			this.name = name;
			this.angle = angle;
			this.x = x;
			this.y = y;
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Float getAngle() {
			return angle;
		}
		public void setAngle(Float angle) {
			this.angle = angle;
		}
		public Float getX() {
			return x;
		}
		public void setX(Float x) {
			this.x = x;
		}
		public Float getY() {
			return y;
		}
		public void setY(Float y) {
			this.y = y;
		}
	}
	
	public static class ChallengesConverter implements Converter {

		@SuppressWarnings("rawtypes")
		public boolean canConvert(Class type) {
			return TreeMap.class.isAssignableFrom(type);
		}

		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			@SuppressWarnings("unchecked")
			TreeMap<String,Challenge> challenges = (TreeMap<String,Challenge>) source;
			for (Entry<String,Challenge> entry : challenges.entrySet()) {
				writer.startNode(Challenge.class.getSimpleName().toLowerCase());
				context.convertAnother(entry.getValue());
				writer.endNode();
			}
		}

		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			TreeMap<Integer,Challenge> challenges = new TreeMap<Integer,Challenge>();
			while (reader.hasMoreChildren()) {
				reader.moveDown();
				Challenge challenge = (Challenge) context.convertAnother(challenges, Challenge.class);
				challenges.put(challenge.getId(), challenge);
				reader.moveUp();
			}
			return challenges;
		}
	}
	
	public static class SolutionsConverter implements Converter {

		@SuppressWarnings("rawtypes")
		public boolean canConvert(Class type) {
			return TreeMap.class.isAssignableFrom(type);
		}

		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			@SuppressWarnings("unchecked")
			TreeMap<String,Solution> solutions = (TreeMap<String,Solution>) source;
			for (Entry<String,Solution> entry : solutions.entrySet()) {
				writer.startNode(Solution.class.getSimpleName().toLowerCase());
				context.convertAnother(entry.getValue());
				writer.endNode();
			}
		}

		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			TreeMap<String,Solution> solutions = new TreeMap<String,Solution>();
			while (reader.hasMoreChildren()) {
				reader.moveDown();
				Solution solution = (Solution) context.convertAnother(solutions, Solution.class);
				solutions.put(solution.getIdref(), solution);
				reader.moveUp();
			}
			return solutions;
		}
	}
	
	public void init() {
		if (this.challenges == null) this.challenges = new TreeMap<Integer,Challenge>();
		if (this.solutions == null) this.solutions = new TreeMap<String,Solution>();
	}

	@XStreamConverter(ChallengesConverter.class)
	private TreeMap<Integer,Challenge> challenges;
	
	@XStreamConverter(SolutionsConverter.class)
	private TreeMap<String,Solution> solutions;
	
	public TreeMap<Integer, Challenge> getChallenges() {
		return challenges;
	}
	public void setChallenges(TreeMap<Integer, Challenge> challenges) {
		this.challenges = challenges;
	}
	public TreeMap<String, Solution> getSolutions() {
		return solutions;
	}
	public void setSolutions(TreeMap<String, Solution> solutions) {
		this.solutions = solutions;
	}
	
	public void removeChallenge(Challenge challenge) {
		if (challenges != null) {
			
			Integer id = challenge.getId();
			challenges.remove(id);
		
			// clean up any referring solutions
			if (solutions != null) {
				String solutionsPrefix = id + ".";
				boolean needsCleaning = true;
				while(needsCleaning) {
					needsCleaning = false;
					for (Entry<String,Solution> entry  : solutions.entrySet()) {
						if (entry.getKey().startsWith(solutionsPrefix)) {
							solutions.remove(entry.getKey());
							needsCleaning = true;
							break;
						}
					}
				}
			}
		}
	}
	
	public void addLibraryEntry(LibraryEntry libraryEntry) {
		if (libraryEntry instanceof Challenge) {
			Challenge challenge = (Challenge) libraryEntry;
			if (challenges == null) {
				challenges = new TreeMap<Integer,Challenge>();
			}
			challenges.put(challenge.getId(), challenge);
		}
		else if (libraryEntry instanceof Solution) {
			Solution solution = (Solution) libraryEntry;
			if (solutions == null) {
				solutions = new TreeMap<String,Solution>();
			}
			
			// enforce a unique solutions key (while maintaining idref references)
			int index = 0;
			for (Entry<String,Solution> entry  : solutions.entrySet()) {
				if (entry.getKey().startsWith(solution.getIdref() + ".")) {
					index++;
				}
			}
			String key = solution.getIdref() + "." + index;
			solution.setIdref(key);
			solutions.put(key, solution);
		}
	}
	
	public List<Solution> getSolutions(Challenge challenge) {
		String solutionsPrefix = challenge.getId() + ".";
		List<Solution> solutionList = new ArrayList<Solution>();
		for (Entry<String,Solution> entry  : solutions.entrySet()) {
			if (entry.getKey().startsWith(solutionsPrefix)) {
				solutionList.add(entry.getValue());
			}
		}
		return solutionList;
	}
}
