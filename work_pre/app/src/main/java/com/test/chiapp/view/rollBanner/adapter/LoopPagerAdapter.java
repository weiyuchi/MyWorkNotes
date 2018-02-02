package com.test.chiapp.view.rollBanner.adapter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView.ScaleType;

import com.test.chiapp.view.rollBanner.HintView;
import com.test.chiapp.view.rollBanner.ImageLoader;
import com.test.chiapp.view.rollBanner.RollPagerView;
import com.test.chiapp.view.rollBanner.TagImageView;

/**
 * 作者：WYC
 * 时间：2017.11.24
 */
public class LoopPagerAdapter extends PagerAdapter{
	
	
    private RollPagerView mViewPager;

    private ArrayList<TagImageView> mViewList = new ArrayList<TagImageView>();
    
    private List<String> pathList = new ArrayList<String>();
    private int[] pathList_loc = null;
    
    private boolean isNet = false;
    /**
     * 判断图片是否是网络图片
     */
    private String imageType ="";
    private class LoopHintViewDelegate implements RollPagerView.HintViewDelegate{
        @Override
        public void setCurrentPosition(int position, HintView hintView) {
            if (hintView!=null&&getRealCount()>0)
                hintView.setCurrent(position%getRealCount());
        }

        @Override
        public void initView(int length, int gravity, HintView hintView) {
            if (hintView!=null)
                hintView.initView(getRealCount(),gravity);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        mViewList.clear();
        initPosition();
        super.notifyDataSetChanged();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
        initPosition();
    }

    private void initPosition(){
        if (mViewPager.getViewPager().getCurrentItem() == 0&&getRealCount()>0){
            int half = Integer.MAX_VALUE/2;
            int start = half - half%getRealCount();
            setCurrent(start);
        }
    }

    private void setCurrent(int index){
        try {
            Field field = ViewPager.class.getDeclaredField("mCurItem");
            field.setAccessible(true);
            field.set(mViewPager.getViewPager(),index);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载网络图片
     */
    public LoopPagerAdapter(RollPagerView viewPager,Context context,List<String> pathList){
    	this.pathList = pathList;
        this.mViewPager = viewPager;
        isNet = true;
        viewPager.setHintViewDelegate(new LoopHintViewDelegate());
    }
    public LoopPagerAdapter(RollPagerView viewPager,Context context,int[] pathList){
    	this.pathList_loc = pathList;
        this.mViewPager = viewPager;
        isNet = false;
        viewPager.setHintViewDelegate(new LoopHintViewDelegate());
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0==arg1;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        int realPosition = position%getRealCount();
        View itemView = findViewByPosition(container,realPosition);
        container.addView(itemView);
        return itemView;
    }


    private View findViewByPosition(ViewGroup container,int position){
    	TagImageView returnView=null;
        for (TagImageView view : mViewList) {
            if (view.getPos() == position&&view.getParent()==null){
            	returnView  = view;
            	break;
            }
        }
        if (returnView == null) {
        	returnView = new TagImageView(container.getContext());
            returnView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            returnView.setScaleType(ScaleType.FIT_XY);
            returnView.setPos(position);
            mViewList.add(returnView);
		}
        if (isNet) {
			ImageLoader.getInstance().LoadImage(pathList.get(position),returnView);//加载网络图片
		}else{
			try {
				returnView.setImageResource(pathList_loc[position]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
        
        return returnView;
    }
    @Deprecated
    @Override
    public final int getCount() {
        return getRealCount()<=0?getRealCount():Integer.MAX_VALUE;
    }

    public int getRealCount(){
    	int returnInt = 0;
    	if (isNet) {
			returnInt = pathList.size();
		}else {
			returnInt = pathList_loc.length;
		}
    	
    	return returnInt;
    };
}
