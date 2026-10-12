package com.itkacher.views

import com.itkacher.data.DebugRequest

/** Coalesce notifications only: response bytes remain in DebugRequest. */
class PendingRequestUpdates {
    private val pending = LinkedHashMap<String, Pair<Long, DebugRequest>>()

    @Synchronized
    fun offer(generation: Long, request: DebugRequest) {
        pending[request.id] = generation to request
    }

    @Synchronized
    fun drain(generation: Long, limit: Int = 100): List<DebugRequest> {
        val result = ArrayList<DebugRequest>()
        val iterator = pending.values.iterator()
        while (iterator.hasNext() && result.size < limit) {
            val (epoch, request) = iterator.next()
            iterator.remove()
            if (epoch == generation) result.add(request)
        }
        return result
    }

    @Synchronized
    fun clear() = pending.clear()
}
