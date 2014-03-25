package com.google.code.twig.standard;

import com.google.code.twig.test.festival.Band;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.test.space.Mission;
import com.google.code.twig.test.space.Pilot;
import com.google.code.twig.test.space.RocketShip;
import com.google.code.twig.test.space.SpaceStation;
import com.google.code.twig.test.space.RocketShip.Planet;

import java.util.List;
import java.util.logging.Logger;

import static com.google.code.twig.standard.TranslatorObjectDatastoreTest.BandBuilder.aNewBand;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TranslatorObjectDatastoreTest extends LocalDatastoreTestCase {

  private static final Logger log = Logger.getLogger(TranslatorObjectDatastoreTest.class.getName());

  private AnnotationObjectDatastore datastore;

	@Before
	public void createDatastore() {
		this.datastore = new AnnotationObjectDatastore();
	}

	@Test
	public void associateObjectWithSameKey() {
		// create and store a station
		SpaceStation station = new SpaceStation("behemoth");
		datastore.store(station);
		
		// associating a new station with the same key will return the same instance
		SpaceStation associated = datastore.associate(new SpaceStation("behemoth"));
		Assert.assertSame(station, associated);
	}

  @Test
  public void associateGraphWithSameKey() {
    // create and store a station
    SpaceStation station = new SpaceStation("behemoth");
    datastore.store(station);

    // associating an instance that references an unassociated station
    // should throw an exception.
    Mission exploration = new Mission("Exploration");
    SpaceStation explorers = new SpaceStation("explorers");
    exploration.setStation(explorers);

    datastore.associate(exploration);

    Assert.assertFalse(datastore.isAssociated(explorers));

    // associating an instance that references an associated station
    Mission domination = new Mission("Domination");
    SpaceStation dominstation = new SpaceStation("behemoth");

    Assert.assertNotSame(dominstation, station);
    dominstation = datastore.associate(dominstation);
    Assert.assertSame(dominstation, station);

    domination.setStation(dominstation);

    // now associate a new mission which references existing station
    Mission associated = datastore.associate(domination);

    // the same instance should be returned when not already associated
    Assert.assertSame(associated, domination);

    // just check that the station is still the same one
    Assert.assertSame(station, associated.getStation());
  }

  @Test
  public void denormalise() throws EntityNotFoundException {
    Mission mission = new Mission("conquor");
    mission.getPilots().add(new Pilot("bob", new RocketShip(Planet.MARS)));

    Key key = datastore.store(mission);

    // check we have only the right amount of properties stored
    Entity entity = datastore.getDefaultService().get(key);
    Assert.assertEquals(5, entity.getProperties().size());

    datastore.disassociateAll();

    Mission loaded = datastore.load().key(key).activate(0).now();

    Assert.assertFalse(datastore.isActivatable(mission));

    Pilot pilot = loaded.getPilots().iterator().next();

    Assert.assertFalse(datastore.isActivated(pilot));

    // the pilot is unactivated but its name was set
    Assert.assertEquals(pilot.getName(), "bob");

    Assert.assertEquals(pilot.getSpaceship().getDestination(), RocketShip.Planet.MARS);
  }

  @Test
  public void noEntityGroupsWhenCreatingTransactionForTheFirstTime() {

    datastore.beginTransaction();

    List<Key> keys = datastore.logTransactionEntityGroups();

    assertThat(keys.size(), is(0));

    datastore.getTransaction().commit();
  }

  @Test
  public void storeMoreThanFiveEntityGroups() {

    datastore.beginTransaction();

    try {

      datastore.store(aNewBand().withName("Band 1").build());
      datastore.store(aNewBand().withName("Band 2").build());
      datastore.store(aNewBand().withName("Band 3").build());
      datastore.store(aNewBand().withName("Band 4").build());
      datastore.store(aNewBand().withName("Band 5").build());
      datastore.store(aNewBand().withName("Band 6").build());

    } catch (IllegalArgumentException e) {

      log.info("Exception: " + e.getMessage());

      List<Key> entityGroupsKeys = datastore.logTransactionEntityGroups();

      assertThat(entityGroupsKeys.size(), is(5));
    }

    datastore.getTransaction().commit();
  }

  @Test
  public void executeManyTransactionConsequently() {
    
    datastore.beginTransaction();

    datastore.store(aNewBand().withName("Band 1").build());
    datastore.store(aNewBand().withName("Band 2").build());

    datastore.getTransaction().commit();


    datastore.beginTransaction();

    Band band = aNewBand().withName("Band 3").build();
    Key bandKey = datastore.store(band);

    List<Key> keys = datastore.logTransactionEntityGroups();

    datastore.getTransaction().commit();

    assertThat(keys.size(), is(1));
    assertThat(keys.get(0), is(equalTo(bandKey)));
  }

  @Test
  public void loadByKeyMoreThanFiveEntitiesInTransaction() {

    Band firstBand = aNewBand().withName("First Band").build();
    Key firstBandKey = datastore.store(firstBand);

    Band secondBand = aNewBand().withName("Second Band").build();
    Key secondBandKey = datastore.store(secondBand);

    Band thirdBand = aNewBand().withName("Third Band").build();
    Key thirdBandKey = datastore.store(thirdBand);

    Band fourthBand = aNewBand().withName("Fourth Band").build();
    Key fourthBandKey = datastore.store(fourthBand);

    Band fifthBand = aNewBand().withName("Fifth Band").build();
    Key fifthBandKey = datastore.store(fifthBand);

    Band sixthBand = aNewBand().withName("Sixth Band").build();
    Key sixthKeyBand = datastore.store(sixthBand);

    datastore.disassociateAll();


    datastore.beginTransaction();

    try {

      datastore.load(firstBandKey);
      datastore.load(secondBandKey);
      datastore.load(thirdBandKey);
      datastore.load(fourthBandKey);
      datastore.load(fifthBandKey);
      datastore.load(sixthKeyBand);

    } catch (IllegalArgumentException e) {

      log.info(e.getMessage());

      List<Key> keys = datastore.logTransactionEntityGroups();

      assertThat(keys.size(), is(5));
    }

    datastore.getTransaction().commit();
  }

  @Test
  public void loadByClassAndTypeMoreThanFiveEntitiesInTransaction() {

    Band firstBand = aNewBand().withName("First Band").build();
    datastore.store(firstBand);

    Band secondBand = aNewBand().withName("Second Band").build();
    datastore.store(secondBand);

    Band thirdBand = aNewBand().withName("Third Band").build();
    datastore.store(thirdBand);

    Band fourthBand = aNewBand().withName("Fourth Band").build();
    datastore.store(fourthBand);

    Band fifthBand = aNewBand().withName("Fifth Band").build();
    datastore.store(fifthBand);

    Band sixthBand = aNewBand().withName("Sixth Band").build();
    datastore.store(sixthBand);

    datastore.disassociateAll();

    datastore.beginTransaction();

    try {

      datastore.load(Band.class, firstBand.getName());
      datastore.load(Band.class, secondBand.getName());
      datastore.load(Band.class, thirdBand.getName());
      datastore.load(Band.class, fourthBand.getName());
      datastore.load(Band.class, fifthBand.getName());
      datastore.load(Band.class, sixthBand.getName());

    } catch (IllegalArgumentException e) {

      log.info(e.getMessage());

      List<Key> keys = datastore.logTransactionEntityGroups();

      assertThat(keys.size(), is(5));
    }

    datastore.getTransaction().commit();
  }

  @Test
  public void deleteMoreThanFiveEntitiesConsequentlyInTransaction() {

    Band firstBand = aNewBand().withName("First Band").build();
    datastore.store(firstBand);

    Band secondBand = aNewBand().withName("Second Band").build();
    datastore.store(secondBand);

    Band thirdBand = aNewBand().withName("Third Band").build();
    datastore.store(thirdBand);

    Band fourthBand = aNewBand().withName("Fourth Band").build();
    datastore.store(fourthBand);

    Band fifthBand = aNewBand().withName("Fifth Band").build();
    datastore.store(fifthBand);

    Band sixthBand = aNewBand().withName("Sixth Band").build();
    datastore.store(sixthBand);

    datastore.beginTransaction();

    try {

      datastore.delete(firstBand);
      datastore.delete(secondBand);
      datastore.delete(thirdBand);
      datastore.delete(fourthBand);
      datastore.delete(fifthBand);
      datastore.delete(sixthBand);

    } catch (IllegalArgumentException e) {

      log.info(e.getMessage());

      List<Key> keys = datastore.logTransactionEntityGroups();

      assertThat(keys.size(), is(5));
    }

    datastore.getTransaction().commit();
  }

  static class BandBuilder {
    
    private String name;
    
    public static BandBuilder aNewBand() {
      return new BandBuilder();
    }
    
    public BandBuilder withName(String name) {
      this.name = name;
      return this;
    }
    
    public Band build() {
      
      Band band = new Band();
      band.setName(name);
      
      return band;
    }
  }
}

