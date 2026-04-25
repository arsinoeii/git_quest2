package com.questboard.widget

import android.content.Intent
import android.widget.RemoteViewsService

class TaskListRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TaskListRemoteViewsFactory(applicationContext)
    }
}
