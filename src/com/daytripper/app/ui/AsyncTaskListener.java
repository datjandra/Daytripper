package com.daytripper.app.ui;

public interface AsyncTaskListener<R> {
	public void taskDone(R result);
}
