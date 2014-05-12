package com.google.code.twig;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.code.twig.annotation.AnnotationConfiguration;
import com.google.code.twig.standard.StandardObjectDatastore;
import org.junit.After;
import org.junit.Before;

public abstract class LocalDatastoreTestCase {

  private final LocalServiceTestHelper helper =

          new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig().setDefaultHighRepJobPolicyUnappliedJobPercentage(0.01f));

  @Before
  public void setupDatastore() {
    helper.setUp();
  }

  @After
  public void tearDownDatastore() {
    helper.tearDown();
  }

  public StandardObjectDatastore createDatastoreInstance() {
    Settings settings = Settings.builder().build();
    return new StandardObjectDatastore(settings, new AnnotationConfiguration(), 1000, true);
  }
}