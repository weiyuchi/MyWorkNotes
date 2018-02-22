package com.test.chiapp.view.roll_banner.inter;


public interface HintView {

	void initView(int length, int gravity);

	void setCurrent(int current);

	/**
     * Created by zhuchenxi on 16/8/4.
     */
	interface OnItemClickListener {
        void onItemClick(int position);
    }
}

