package gmt.snowman.collection

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object SortedMap {

    def empty[K, V]: SortedMap[K, V] = new SortedMap[K, V]
}

class SortedMap[K, V] extends Iterable[V] {

    private val _map = mutable.Map.empty[K, V]
    private val _list = ListBuffer.empty[V]

    def put(key: K, value: V): Option[V] = {
        _map.put(key, value) match {
            case s @ Some(v) =>
                removeValueFromList(v)
                _list.append(value)
                s
            case None =>
                _list.append(value)
                None
        }
    }

    def get(key: K): Option[V] = _map.get(key)

    def contains(key: K): Boolean = _map.contains(key)

    def remove(key: K): Option[V] = {
        _map.remove(key) match {
            case s @ Some(v) =>
                removeValueFromList(v)
                s
            case None =>
                None
        }

    }

    private def removeValueFromList(v: V): Unit ={
        var removed = false
        val it = _list.iterator

        var i = 0

        while (!removed && it.hasNext) {
            if (it.next() == v) {
                removed = true
            } else {
                i += 1
            }
        }

        if (removed) {
            _list.remove(i)
        }
    }

    override def iterator: Iterator[V] = _list.iterator
}

