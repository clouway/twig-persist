package com.google.code.twig;

import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.annotation.Id;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Georgi Georgiev (GeorgievJon@gmail.com)
 */
public class LoadEntityWithPropertyFromSetTest extends LocalDatastoreTestCase {

  static class Foo {

    @Id
    private Long id;

    private Set<String> stringSet;

    private Set<Bar> bars;
  }

  static class Bar {

    @Id
    private String id;

  }

  private ObjectDatastore datastore;

  @Before
  public void setUp() throws Exception {

    datastore = new AnnotationObjectDatastore(Settings.builder().crossGroupTransactions(false).build());
  }

  @Test
  public void loadEntityWithPropertyFromSet() throws Exception {

    Foo foo = new Foo();
    foo.id = 1l;
    foo.stringSet = Sets.newHashSet("string 1", "string 2");

    datastore.store(foo);
    datastore.disassociateAll();

    datastore.load().type(Foo.class).id(1l).now();

  }


  @Test
  public void loadEntityWithPropertyFromEmptySet() throws Exception {

    Foo foo = new Foo();
    foo.id = 1l;
    foo.stringSet = new HashSet<String>();

    datastore.store(foo);
    datastore.disassociateAll();

    datastore.load().type(Foo.class).id(1l).now();

  }

  @Test
  public void loadEntityWithRelatedEntitiesSet() {

//    Bar bar = new Bar();
//    bar.id = "123";
//
//    Bar bar2 = new Bar();
//    bar2.id = "1234";
//
//    datastore.store().instance(bar).now();
//    datastore.store().instance(bar2).now();
//    datastore.disassociateAll();
//
    Foo foo = new Foo();
    foo.id = 1l;
//
//    foo.bars = new HashSet<Bar>();
//    foo.bars.add(bar);
//    foo.bars.add(bar2);
//
    datastore.store().instance(foo).now();

    datastore.disassociateAll();

    Foo existing = datastore.load().type(Foo.class).id(1l).now();

//    assertThat(existing.bars.size(), is(equalTo(2)));

  }

  @Test
  public void loadEntityWithRelatedEmptySet() {

    Bar bar = new Bar();
    bar.id = "123";

    datastore.store().instance(bar).now();
    datastore.disassociateAll();

    Foo foo = new Foo();
    foo.id = 1l;

    foo.bars = new HashSet<Bar>();

    datastore.store().instance(foo).now();

    datastore.disassociateAll();

    Foo existing = datastore.load().type(Foo.class).id(1l).now();

    assertThat(existing.bars.size(), is(equalTo(0)));

  }

}
