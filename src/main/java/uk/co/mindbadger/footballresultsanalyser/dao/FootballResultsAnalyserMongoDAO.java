package uk.co.mindbadger.footballresultsanalyser.dao;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import uk.co.mindbadger.footballresultsanalyser.domain.Division;
import uk.co.mindbadger.footballresultsanalyser.domain.DomainObjectFactory;
import uk.co.mindbadger.footballresultsanalyser.domain.Fixture;
import uk.co.mindbadger.footballresultsanalyser.domain.Season;
import uk.co.mindbadger.footballresultsanalyser.domain.SeasonDivision;
import uk.co.mindbadger.footballresultsanalyser.domain.SeasonDivisionTeam;
import uk.co.mindbadger.footballresultsanalyser.domain.Team;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class FootballResultsAnalyserMongoDAO implements	FootballResultsAnalyserDAO<String> {
	private static final String MONGO_SEASON = "season";
	private static final String MONGO_TEAM = "team";
	private static final String MONGO_DIVISION = "division";
	private static final String MONGO_FIXTURE = "fixture";

	private static final String ID = "_id";
	private static final String DIV_ID = "div_id";
	private static final String AWAY_GOALS = "away_goals";
	private static final String HOME_GOALS = "home_goals";
	private static final String DIV_NAME = "div_name";
	private static final String FIXTURE_DATE = "fixture_date";
	private static final String AWAY_TEAM_ID = "away_team_id";
	private static final String HOME_TEAM_ID = "home_team_id";
	private static final String TEAM_NAME = "team_name";
	private static final String SSN_NUM = "ssn_num";

	Logger logger = Logger.getLogger(FootballResultsAnalyserMongoDAO.class);

	private DomainObjectFactory<String> domainObjectFactory;
	private String dbName;
	private String mongoHost;
	
	private DB db;
	private MongoClient mongoClient;
	
	private class KV {
		private String key;
		private Object value;
		public KV (String key, Object value) {
			this.key = key;
			this.value = value;
		}
		public String getKey () {
			return key;
		}
		public Object getValue () {
			return value;
		}
	}
	
	private KV kv(String key, Object value) {
		return new KV(key, value);
	}
	
	private String addMongoRecord (String collection, KV ... values) {
		DBCollection mongoCollection = db.getCollection(collection);
		BasicDBObject basicObject = new BasicDBObject ();
		for (int i = 0; i < values.length; i++) {
			basicObject.append(values[i].getKey(), values[i].getValue());
		}
		mongoCollection.insert(basicObject);
		Object objectId = basicObject.get(ID);
		return objectId.toString();
	}
	
	@Override
	public Division<String> addDivision(String divisionName) {
		String divId = addMongoRecord(MONGO_DIVISION, kv(DIV_NAME, divisionName));
		
//		DBCollection mongoDivisions = db.getCollection(MONGO_DIVISION);
//		BasicDBObject mongoDivision = new BasicDBObject(DIV_NAME, divisionName); 
//		mongoDivisions.insert(mongoDivision);
//		ObjectId id = (ObjectId)mongoDivision.get( ID );
		
		Division<String> division = domainObjectFactory.createDivision(divisionName);
		division.setDivisionId(divId);
		
		return division;
	}

	@Override
	public Fixture<String> addFixture(Season<String> season, Calendar fixtureDate, Division<String> division, Team<String> homeTeam, Team<String> awayTeam, Integer homeGoals, Integer awayGoals) {
		String fixtureId = addMongoRecord(MONGO_FIXTURE, kv(SSN_NUM, season.getSeasonNumber()),
				kv(HOME_TEAM_ID, homeTeam.getTeamId()),
				kv(AWAY_TEAM_ID, awayTeam.getTeamId()),
				kv(FIXTURE_DATE, fixtureDate.getTime()),
				kv(DIV_ID, division.getDivisionId()),
				kv(HOME_GOALS, homeGoals),
				kv(AWAY_GOALS, awayGoals));
		
//		DBCollection mongoFixtures = db.getCollection(MONGO_FIXTURE);
//		BasicDBObject mongoFixture = new BasicDBObject(SSN_NUM, season.getSeasonNumber())
//			.append(HOME_TEAM_ID, homeTeam.getTeamId())
//			.append(AWAY_TEAM_ID, awayTeam.getTeamId())
//			.append(FIXTURE_DATE, fixtureDate.getTime())
//			.append(DIV_ID, division.getDivisionId())
//			.append(HOME_GOALS, homeGoals)
//			.append(AWAY_GOALS, awayGoals)	;
//		mongoFixtures.insert(mongoFixture);
//		ObjectId id = (ObjectId)mongoFixture.get( ID );
		
		Fixture<String> fixture = domainObjectFactory.createFixture(season, homeTeam, awayTeam);
		fixture.setDivision(division);
		fixture.setFixtureDate(fixtureDate);
		fixture.setHomeGoals(homeGoals);
		fixture.setAwayGoals(awayGoals);
		fixture.setFixtureId(fixtureId);

		return fixture;
	}

	@Override
	public Season<String> addSeason(Integer seasonNumber) {
		addMongoRecord(MONGO_SEASON, kv(ID, seasonNumber));
//		DBCollection mongoSeasons = db.getCollection("season");
//		BasicDBObject mongoSeason = new BasicDBObject(ID, seasonNumber);
//		mongoSeasons.insert(mongoSeason);
		
		Season<String> season = domainObjectFactory.createSeason(seasonNumber);
		
		return season;
	}

	@Override
	public Team<String> addTeam(String teamName) {
		String teamId = addMongoRecord(MONGO_TEAM, kv(TEAM_NAME, teamName));
//		DBCollection mongoTeams = db.getCollection(MONGO_TEAM);
//		BasicDBObject mongoTeam = new BasicDBObject(TEAM_NAME, teamName); 
//		mongoTeams.insert(mongoTeam);
//		ObjectId id = (ObjectId)mongoTeam.get( ID );
		
		Team<String> team = domainObjectFactory.createTeam(teamName);
		team.setTeamId(teamId);
		
		return team;
	}

	@Override
	public void closeSession() {
		mongoClient.close();
	}

	private Division<String> mapMongoToDivision (DBObject mongoObject) {
		String divName = mongoObject.get(DIV_NAME).toString();
		String divId = mongoObject.get(ID).toString();
		Division<String> division = domainObjectFactory.createDivision(divName);
		division.setDivisionId(divId);
		return division;
	}
	
	@Override
	public Map<String, Division<String>> getAllDivisions() {		
		Map<String, Division<String>> divisions = new HashMap<String, Division<String>> ();
		
		DBCollection mongoDivisions = db.getCollection(MONGO_DIVISION);		
		DBCursor divisionsCursor = mongoDivisions.find();
		
		while(divisionsCursor.hasNext()) {
			DBObject divisionObject = divisionsCursor.next();
			Division<String> division = mapMongoToDivision (divisionObject);
			divisions.put(division.getDivisionId(), division);
		}
		
		return divisions;
	}

	private Team<String> mapMongoToTeam (DBObject mongoObject) {
		String teamName = mongoObject.get(TEAM_NAME).toString();
		String teamId = mongoObject.get(ID).toString();
		Team<String> team = domainObjectFactory.createTeam(teamName);
		team.setTeamId(teamId);
		return team;
	}
	
	@Override
	public Map<String, Team<String>> getAllTeams() {
		Map<String, Team<String>> teams = new HashMap<String, Team<String>> ();
		
		DBCollection mongoTeams = db.getCollection(MONGO_TEAM);		
		DBCursor teamsCursor = mongoTeams.find();
		
		while(teamsCursor.hasNext()) {
			DBObject teamObject = teamsCursor.next();
			Team<String> newTeam = mapMongoToTeam (teamObject);
			teams.put(newTeam.getTeamId(), newTeam);
		}
		
		return teams;
	}

//	@Override
//	public Set<SeasonDivision<String>> getDivisionsForSeason(int seasonNumber) {
//		throw new RuntimeException("Not implemented yet!");
//	}

	@Override
	public List<Fixture<String>> getFixturesForTeamInDivisionInSeason(Season<String> season, Division<String> division, Team<String> team) {
		throw new RuntimeException("Not implemented yet!");
	}

	private Fixture<String> mapMongoToFixture (DBObject mongoObject) {
		String seasonNumber = mongoObject.get(SSN_NUM).toString();
		String homeTeamId = mongoObject.get(HOME_TEAM_ID).toString();
		String awayTeamId = mongoObject.get(AWAY_TEAM_ID).toString();
		
		String fixtureDateAsString = mongoObject.get(FIXTURE_DATE).toString();
		Calendar fixtureDate = Calendar.getInstance(); //TODO convert the string into a data
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
		try {
			fixtureDate.setTime(sdf.parse(fixtureDateAsString));
		} catch (ParseException e) {
			throw new RuntimeException (e);
		}
		
		String divId = mongoObject.get(DIV_ID).toString();
		Integer homeGoals = Integer.parseInt(mongoObject.get(HOME_GOALS).toString());
		Integer awayGoals = Integer.parseInt(mongoObject.get(AWAY_GOALS).toString());
		
		Season<String> season = getSeason(Integer.parseInt(seasonNumber));
		Team<String> homeTeam = getTeam(homeTeamId);
		Team<String> awayTeam = getTeam(awayTeamId);
		Division<String> division = getDivision(divId);
		
		Fixture<String> fixture = domainObjectFactory.createFixture(season, homeTeam, awayTeam);
		fixture.setDivision(division);
		fixture.setFixtureDate(fixtureDate);
		fixture.setHomeGoals(homeGoals);
		fixture.setAwayGoals(awayGoals);
		
		return fixture;
	}
	
	@Override
	public List<Fixture<String>> getFixturesWithNoFixtureDate() {
		List<Fixture<String>> fixtures = new ArrayList<Fixture<String>> ();
		
		DBCollection mongoFixures = db.getCollection(MONGO_FIXTURE);
		
		DBObject noFixtureDate = new BasicDBObject(FIXTURE_DATE,new BasicDBObject("$exists", false));
		DBObject fixtureDateIsNull = new BasicDBObject(FIXTURE_DATE, null);
		
		BasicDBList or = new BasicDBList();
		or.add(noFixtureDate);
		or.add(fixtureDateIsNull);
		
		DBObject query = new BasicDBObject("$or", or);
		
		DBCursor fixturesCursor = mongoFixures.find(query);
		
		while(fixturesCursor.hasNext()) {
			DBObject fixtureObject = fixturesCursor.next();
			Fixture<String> fixture = mapMongoToFixture (fixtureObject);
			
//			String seasonNumber = (String) fixtureObject.get(SSN_NUM);
//			String homeTeamId = (String) fixtureObject.get(HOME_TEAM_ID);
//			String awayTeamId = (String) fixtureObject.get(AWAY_TEAM_ID);
//			String fixtureDate = (String) fixtureObject.get(FIXTURE_DATE);
//			String divId = (String) fixtureObject.get(DIV_ID);
//			String homeGoals = (String) fixtureObject.get(HOME_GOALS);
//			String awayGoals = (String) fixtureObject.get(AWAY_GOALS);
			//Fixture<String> fixture = domainObjectFactory.createFixture(arg0, arg1, arg2);

			fixtures.add(fixture);
		}
		
		return fixtures;
	}

	private Season<String> mapMongoToSeason (DBObject mongoObject) {
		Integer seasonNumber = (Integer) mongoObject.get(ID);
		Season<String> season = domainObjectFactory.createSeason(seasonNumber);
		return season;
	}
	
	@Override
	public Season<String> getSeason(Integer seasonNumber) {
		DBCollection mongoSeasons = db.getCollection(MONGO_SEASON);
		DBObject query = new BasicDBObject(ID, seasonNumber);
		DBCursor seasonsCursor = mongoSeasons.find(query);
		if(seasonsCursor.hasNext()) {
			DBObject seasonObject = seasonsCursor.next();
			return mapMongoToSeason(seasonObject);
		}
		return null;
	}

	@Override
	public Division<String> getDivision(String divId) {
		DBCollection mongoDivisions = db.getCollection(MONGO_DIVISION);
		DBObject query = new BasicDBObject(ID, divId);
		DBCursor divisionsCursor = mongoDivisions.find(query);
		if(divisionsCursor.hasNext()) {
			DBObject divisionObject = divisionsCursor.next();
			return mapMongoToDivision(divisionObject);
		}
		return null;
	}
	
	@Override
	public Team<String> getTeam(String teamId) {
		DBCollection mongoTeams = db.getCollection(MONGO_TEAM);
		DBObject query = new BasicDBObject(ID, teamId);
		DBCursor teamsCursor = mongoTeams.find(query);
		if(teamsCursor.hasNext()) {
			DBObject teamObject = teamsCursor.next();
			return mapMongoToTeam(teamObject);
		}
		return null;
	}

	@Override
	public List<Season<String>> getSeasons() {
		List<Season<String>> seasons = new ArrayList<Season<String>> ();
		
		DBCollection mongoSeasons = db.getCollection(MONGO_SEASON);		
		DBCursor seasonsCursor = mongoSeasons.find();
		
		while(seasonsCursor.hasNext()) {
			DBObject seasonObject = seasonsCursor.next();
			Season<String> newSeason = mapMongoToSeason (seasonObject);
			seasons.add(newSeason);
		}
		
		return seasons;	
	}

//	@Override
//	public Set<SeasonDivisionTeam<String>> getTeamsForDivisionInSeason(int arg0,int arg1) {
//		throw new RuntimeException("Not implemented yet!");
//	}

	@Override
	public List<Fixture<String>> getUnplayedFixturesBeforeToday() {
		List<Fixture<String>> fixtures = new ArrayList<Fixture<String>> ();
		
		DBCollection mongoFixures = db.getCollection(MONGO_FIXTURE);
		
		Calendar today = Calendar.getInstance();
		
		DBObject query = new BasicDBObject();
		query.put (FIXTURE_DATE,new BasicDBObject("$exists", true));
		query.put (FIXTURE_DATE, new BasicDBObject ("$lt", today.getTime()));
		
		DBCursor fixturesCursor = mongoFixures.find(query);
		
		while(fixturesCursor.hasNext()) {
			DBObject fixtureObject = fixturesCursor.next();
			Fixture<String> fixture = mapMongoToFixture (fixtureObject);
			fixtures.add(fixture);
		}
		
		return fixtures;
	}

	@Override
	public void startSession() {
		try {
			mongoClient = new MongoClient(this.mongoHost);
			db = mongoClient.getDB(this.dbName);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException(e);
		}			
	}

	public DomainObjectFactory<String> getDomainObjectFactory() {
		return domainObjectFactory;
	}

	public void setDomainObjectFactory(DomainObjectFactory<String> domainObjectFactory) {
		this.domainObjectFactory = domainObjectFactory;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getMongoHost() {
		return mongoHost;
	}

	public void setMongoHost(String mongoHost) {
		this.mongoHost = mongoHost;
	}

}
