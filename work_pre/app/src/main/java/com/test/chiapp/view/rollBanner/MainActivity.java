package com.test.chiapp.view.rollBanner;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.test.chiapp.R;
import com.test.chiapp.view.rollBanner.adapter.LoopPagerAdapter;
import com.test.chiapp.view.rollBanner.hintview.IconHintView;


public class MainActivity extends Activity {
	private RollPagerView rollpagerView;
	private List<String> list = new ArrayList<String>();
	private int loc[]={R.drawable.banner_1,R.drawable.banner_2,R.drawable.banner_3};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		list.add("https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=3553020298,1715141379&fm=27&gp=0.jpg");
		list.add("https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=526348746,357933766&fm=27&gp=0.jpg");
		list.add("https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=3356121154,1179528716&fm=27&gp=0.jpg");
		rollpagerView = (RollPagerView) findViewById(R.id.my_banner);
//		rollpagerView.setHintView(new TextHintView(this)); //文字
//		rollpagerView.setHintView(new ColorPointHintView(this, Color.YELLOW,Color.WHITE));//圆点
		rollpagerView.setHintView(new IconHintView(this,R.drawable.point_focus,R.drawable.point_normal));//自定义图片
		rollpagerView.setGravity(2); 
		rollpagerView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				Toast.makeText(MainActivity.this, position + "", Toast.LENGTH_SHORT).show();
			}
		});
		LoopPagerAdapter adapter = new LoopPagerAdapter(rollpagerView, MainActivity.this, loc);
		rollpagerView.setAdapter(adapter);
	}
}
