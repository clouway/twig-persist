package com.vercer.engine.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.vercer.cache.Cache;
import com.vercer.cache.CacheItem;
import com.vercer.engine.persist.util.io.NoDescriptorObjectInputStream;
import com.vercer.engine.persist.util.io.NoDescriptorObjectOutputStream;
import com.vercer.rooms.manage.Performance;
import com.vercer.rooms.site.admin.PerformanceFilter;

public class DatastoreCache<K, V> implements Cache<K, V>
{
	private final String namespace;
	private final DatastoreService datastore;
	private final static String PREFIX = "_cache_";
	private static final String PROPERTY_NAME = "data";
	private static final int BUFFER_SIZE = 10 * 1024;
	private static final String LOCK_PROPERTY_NAME = "lock";
	private static final int RETRIES = 10;
	private static final long RETRY_MILLIS = 5000;

	@Inject
	public DatastoreCache(DatastoreService datastore)
	{
		this(datastore, "default");
	}

	public DatastoreCache(DatastoreService datastore, String namespace)
	{
		this.datastore = datastore;
		this.namespace = namespace;
		
	}

	public void invalidate(K key)
	{
		datastore.delete(createDatastoreKey(key));
	}

	public CacheItem<K, V> item(K key)
	{
		performance().start("DatastoreCache item");
		try
		{
			Entity entity = datastore.get(createDatastoreKey(key));
			if (entity.hasProperty(LOCK_PROPERTY_NAME))
			{
				return null;
			}
			else
			{
				performance().finish("DatastoreCache item");
				return deserialize((Blob) entity.getProperty(PROPERTY_NAME));
			}
		}
		catch (EntityNotFoundException e)
		{
			performance().finish("DatastoreCache item");
			return null;
		}
	}

	private Performance performance()
	{
		return PerformanceFilter.performance.get();
	}

	public void put(K key, V value)
	{
		put(new CacheItem<K, V>(key, value));
	}

	public void put(CacheItem<K, V> item)
	{
		performance().start("DatastoreCache put");
		Key key = createDatastoreKey(item.getKey());
 		Entity entity = new Entity(key);
		entity.setProperty(PROPERTY_NAME, serialize(item));
		datastore.put(entity);
		performance().finish("DatastoreCache put");
	}

	protected Blob serialize(CacheItem<K, V> item)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
			NoDescriptorObjectOutputStream ndos = new NoDescriptorObjectOutputStream(baos);
			ndos.writeObject(item);
			return new Blob(baos.toByteArray());
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	protected CacheItem<K, V> deserialize(Blob blob)
	{
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(blob.getBytes());
			NoDescriptorObjectInputStream ndis = new NoDescriptorObjectInputStream(bais);
			@SuppressWarnings("unchecked")
			CacheItem<K, V> item = (CacheItem<K, V>) ndis.readObject();
			return item;
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public V value(K key)
	{
		CacheItem<K, V> item = item(key);
		if (item == null)
		{
			return null;
		}
		else
		{
			if (item.isValid())
			{
				return item.getValue();
			}
			else
			{
				invalidate(key);
				return null;
			}
		}
	}

	public V value(K key, Callable<CacheItem<K, V>> builder)
	{
		performance().start("DatastoreCache value builder");
		Key dskey = createDatastoreKey(key);
		try
		{
			for (int i = 0; i < RETRIES; i++)
			{
				performance().start("DatastoreCache datastore get");
				Entity entity = datastore.get(dskey);
				performance().finish("DatastoreCache datastore get");
				
				if (entity.hasProperty(LOCK_PROPERTY_NAME))
				{
					Thread.sleep(RETRY_MILLIS);
				}
				else
				{
					Blob blob = (Blob) entity.getProperty(PROPERTY_NAME);
					CacheItem<K,V> item = deserialize(blob);
					if (item.isValid())
					{
						performance().finish("DatastoreCache value builder");
						return item.getValue();
					}
				}
			}
			throw new IllegalStateException("Datastore lock timed out");
		}
		catch (EntityNotFoundException e)
		{
			// fall through to create item
		}
		catch (InterruptedException e)
		{
			throw new IllegalStateException(e);
		}

		// no item was found or it was expired
		// TODO look at doing this in a transaction i.e. separate lock entity

		// lock the datastore so others don't also create item
		Entity lockEntity = new Entity(dskey);
		datastore.put(lockEntity);

		CacheItem<K,V> item;
		try
		{
			item = builder.call();
			put(item);
		}
		catch (Exception e)
		{
			datastore.delete(dskey); // remove the lock entity
			throw new IllegalStateException(e);
		}
		performance().finish("DatastoreCache value builder");
		return item.getValue();
	}

	private Key createDatastoreKey(K value)
	{
		return KeyFactory.createKey(PREFIX + namespace, keyName(value));
	}

	protected String keyName(K value)
	{
		return value.toString();
	}

}
