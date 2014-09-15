package com.google.appengine.api.datastore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.apphosting.api.DatastorePb.Query.Order;
import com.google.appengine.api.datastore.Query.SortDirection;

public class EntityComparatorAccessor
{
	public static Comparator<Entity> newEntityComparator(List<SortPredicate> sorts)
	{

    List<Order> orders = new ArrayList<Order>();

    for (SortPredicate sort : sorts) {

      SortDirection sortDirection = sort.getDirection();

      Order order = new Order();

      if (SortDirection.ASCENDING.equals(sortDirection)) {
        order.setDirection(Order.Direction.ASCENDING);

      } else if (SortDirection.DESCENDING.equals(sortDirection)) {
        order.setDirection(Order.Direction.DESCENDING);
      }

      orders.add(order);
    }

    return new EntityComparator(orders);
	}
}
