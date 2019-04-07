package com.github.qingmei2.mvi.base.view.adapter

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.uber.autodispose.lifecycle.CorrespondingEventsFunction
import com.uber.autodispose.lifecycle.LifecycleEndedException
import com.uber.autodispose.lifecycle.LifecycleScopeProvider
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

@Suppress("LeakingThis")
abstract class AutoDisposePagedListAdapter<T : Any, VH : RecyclerView.ViewHolder>(
    lifecycleOwner: LifecycleOwner,
    differ: DiffUtil.ItemCallback<T>
) : PagedListAdapter<T, VH>(differ), DefaultLifecycleObserver, AutoDisposeViewHolderEventsProvider,
    LifecycleScopeProvider<AutoDisposePagedListAdapter.AdapterEvent> {

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    enum class AdapterEvent {
        ON_CREATED, ON_DESTROY
    }

    private val mLifecycleEvents: BehaviorSubject<AutoDisposePagedListAdapter.AdapterEvent> =
        BehaviorSubject.createDefault(AutoDisposePagedListAdapter.AdapterEvent.ON_CREATED)
    private val mViewHolderLifecycleEvents: PublishSubject<AutoDisposeViewHolder.ViewHolderEvent> =
        PublishSubject.create()

    override fun lifecycle(): Observable<AutoDisposePagedListAdapter.AdapterEvent> {
        return mLifecycleEvents.hide()
    }

    override fun correspondingEvents(): CorrespondingEventsFunction<AdapterEvent> {
        return CORRESPONDING_EVENTS
    }

    override fun peekLifecycle(): AdapterEvent? {
        return mLifecycleEvents.value
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        when (holder) {
            is AutoDisposeViewHolder -> postViewHolderEvent(AutoDisposeViewHolder.ViewHolderEvent.OnBinds)
        }
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        when (holder) {
            is AutoDisposeViewHolder -> postViewHolderEvent(
                AutoDisposeViewHolder.ViewHolderEvent.OnUnbindsPosition(
                    holder.adapterPosition
                )
            )
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        mLifecycleEvents.onNext(AutoDisposePagedListAdapter.AdapterEvent.ON_DESTROY)
        postViewHolderEvent(AutoDisposeViewHolder.ViewHolderEvent.OnUnbindsForce)
    }

    private fun postViewHolderEvent(viewHolderEvent: AutoDisposeViewHolder.ViewHolderEvent) {
        mViewHolderLifecycleEvents.onNext(viewHolderEvent)
    }

    override fun providesObservable(): Observable<AutoDisposeViewHolder.ViewHolderEvent> {
        return mViewHolderLifecycleEvents
    }

    companion object {

        private val CORRESPONDING_EVENTS =
            CorrespondingEventsFunction<AutoDisposePagedListAdapter.AdapterEvent> { event ->
                when (event) {
                    AutoDisposePagedListAdapter.AdapterEvent.ON_CREATED ->
                        AutoDisposePagedListAdapter.AdapterEvent.ON_DESTROY
                    else -> throw LifecycleEndedException(
                        "Cannot bind to ViewModel lifecycle after onCleared."
                    )
                }
            }
    }
}