package com.google.code.twig;

import com.google.appengine.api.datastore.Transaction;
import com.google.code.twig.standard.StandardObjectDatastore;
import com.google.code.twig.test.space.SpaceStation;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Georgi Georgiev (GeorgievJon@gmail.com)
 */
public class LoadEntitiesTest extends LocalDatastoreTestCase {

  private StandardObjectDatastore datastore;

  @Before
  public void createDatastore() {
    super.setupDatastore();
    this.datastore  = createDatastoreInstance();
  }

  @Test(expected = IllegalArgumentException.class)
  public void loadEntitiesByKeysInTransaction() throws Exception {

    // create and store a entity
    storeEntities(6);

    datastore.disassociateAll();

    Set<String> keySet = createKeys(6);
    Transaction transaction = datastore.beginTransaction();

    try {
      datastore.loadAll(SpaceStation.class, keySet).values();
    } catch (IllegalArgumentException e ) {
      transaction.rollback();
      throw e;
    }

    fail("missing exception");
  }

  @Test
  public void loadUsingAnotherInstanceOutOfTheTransaction() throws Exception {

    // create and store a entity
    storeEntities(6);

    datastore.disassociateAll();

    Set<String> keySet = createKeys(6);
    Transaction transaction = datastore.beginTransaction();

    StandardObjectDatastore newDatastoreInctance = createDatastoreInstance();

    Collection<SpaceStation> persistedEntities = newDatastoreInctance.loadAll(SpaceStation.class, keySet).values();

    transaction.commit();
    assertThat(persistedEntities.size(), Is.is(6));

  }

  private Set<String> createKeys(int count) {
    Set<String> keySet = new HashSet<String>();
    for (int i = 0; i < count; i++) {
      keySet.add("station" + i);
    }
    return keySet;
  }

  private void storeEntities(int count) {
    for (int i = 0; i < count; i++) {
      datastore.store(new SpaceStation("station" + i));
    }
  }
}
