package com.test.chiapp.view.roll_banner.view;

import android.content.Context;
import android.widget.ImageView;

public class TagImageView extends ImageView {
	private int pos;
	
	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public TagImageView(Context context) {
		super(context);
	}

}
