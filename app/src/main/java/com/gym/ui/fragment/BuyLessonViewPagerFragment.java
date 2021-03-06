package com.gym.ui.fragment;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gym.R;
import com.gym.app.Constants;
import com.gym.bean.BuyLessonBean;
import com.gym.bean.JobInfoBean;
import com.gym.http.image.ImageLoader;
import com.gym.http.protocol.BaseProtocol;
import com.gym.http.protocol.BuyLessonCommonProtocol;
import com.gym.http.protocol.BuyLessonProtocol;
import com.gym.ui.activity.CommentActivity;
import com.gym.ui.activity.ConfirmOrderActivity;
import com.gym.ui.widget.LoadRefreshLayout;
import com.gym.ui.widget.LoadingPage;
import com.gym.utils.ProgressUtil;
import com.gym.utils.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by Administrator on 2015/8/29 0029.
 */
public class BuyLessonViewPagerFragment extends BaseFragment implements View.OnClickListener {
    @InjectView(R.id.common_lv)
    ListView commonLv;
    @InjectView(R.id.nearby)
    TextView nearby;
    @InjectView(R.id.category)
    TextView category;
    @InjectView(R.id.order)
    TextView order;
    @InjectView(R.id.inculdeMenu)
    LinearLayout inculdeMenu;
    @InjectView(R.id.swipe)
    LoadRefreshLayout swipe;
    @InjectView(R.id.chargeMsg)
    EditText chargeMsg;
    @InjectView(R.id.chargeSubmit)
    Button chargeSubmit;
    private int payState;
    private ArrayList<BuyLessonBean> lessonBeans;
    private ArrayList<BuyLessonBean> stateBeans;
    private ViewHolder holder;
    private BuyLessonBean bean;//当前选择的bean
    private boolean loading = false;
    private BuyLessonAdapter adapter;

    public int getPayState() {
        return payState;
    }

    public void setPayState(int payState) {
        this.payState = payState;
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (adapter != null) adapter.notifyDataSetInvalidated();
        }
    };

    @Override
    protected LoadingPage.LoadResult load() {
        clearList();
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("userId", Constants.user.getUsr_UserID() + "");
        BuyLessonProtocol protocol = new BuyLessonProtocol(hashMap);
        lessonBeans = protocol.load(UIUtils.getString(R.string.GetAllOfMyCourse_URL), BaseProtocol.POST);
        seperateState();
        handler.sendEmptyMessage(0);
        return checkResult(stateBeans);
    }

    private void clearList() {
        if (stateBeans != null)
            stateBeans.clear();
        if (lessonBeans != null)
            lessonBeans.clear();
    }

    /**
     * 区分返回数据的状态
     */
    private void seperateState() {
        stateBeans = new ArrayList<>();
        String state = "-1";
        if (getPayState() == 0) {
            stateBeans = lessonBeans;
            return;
        } else if (getPayState() == 1) {
            state = Constants.UNPAY;
        } else if (getPayState() == 2) {
            state = Constants.REFUND;
        } else if (getPayState() == 3) {
            state = Constants.UNASSESS;
        }
        for (BuyLessonBean bean : lessonBeans) {
            if (state.equals(bean.getRemark())) {
                stateBeans.add(bean);
            }
        }
    }

    @Override
    protected View createSuccessView() {
        View rootView = UIUtils.inflate(R.layout.fragment_common_list);
        ButterKnife.inject(this, rootView);
        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipe.setLoading(false);
                swipe.setRefreshing(false);
            }
        });
        operateData();
        return rootView;
    }

    private void operateData() {
        adapter = new BuyLessonAdapter();
        commonLv.setAdapter(adapter);
        commonLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                holder = (ViewHolder) view.getTag();
                bean = stateBeans.get(i);
                operateClick(holder, bean);//处理按钮点击
            }
        });
    }

    private void operateClick(ViewHolder holder, BuyLessonBean bean) {
        holder.course.setOnClickListener(this);
        holder.refund.setOnClickListener(this);
        holder.agreeRefund.setOnClickListener(this);
        holder.pay.setOnClickListener(this);
        holder.cancel.setOnClickListener(this);
        holder.delete.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == holder.agreeRefund) {

        } else if (view == holder.refund) {
            if (!loading)
                new BuyLessonCommonTask().execute(UIUtils.getString(R.string.ApplyRefund_URL));
        } else if (view == holder.course) {
            Intent intent = new Intent(getActivity(), CommentActivity.class);
            intent.putExtra("courseID", bean.getId());
            startActivity(intent);
        } else if (view == holder.pay) {
            Intent intent = new Intent(getActivity(), ConfirmOrderActivity.class);
            JobInfoBean jobInfoBean = new JobInfoBean();
            jobInfoBean.setId(bean.getId());
            jobInfoBean.setJobTitle(bean.getJobTitle());
            jobInfoBean.setTreatment(0d);
            intent.putExtra("bean", jobInfoBean);
            startActivity(intent);
        } else if (view == holder.cancel) {
            if (!loading)
                new BuyLessonCommonTask().execute(UIUtils.getString(R.string.DeleteUserByCourse_URL));
        } else if (view == holder.delete) {
            if (!loading)
                new BuyLessonCommonTask().execute(UIUtils.getString(R.string.DeleteUserByCourse_URL));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshOrLoad();
    }




    class BuyLessonAdapter extends BaseAdapter {


        @Override
        public int getCount() {
            return stateBeans == null ? 0 : stateBeans.size();
        }

        @Override
        public Object getItem(int i) {
            return stateBeans.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = UIUtils.inflate(R.layout.item_buy_lesson);
                viewHolder = new ViewHolder(view);
                view.setTag(viewHolder);
            }
            viewHolder = (ViewHolder) view.getTag();
            BuyLessonBean bean = stateBeans.get(i);
            viewHolder.lessonName.setText(bean.getJobTitle());
            ImageLoader.load(viewHolder.itemImage, bean.getCourse_Photo());
            viewHolder.lessonAddr.setText(bean.getJobAddress());
            if (TextUtils.isEmpty(bean.getJobAddress())) {
                viewHolder.lessonAddr.setVisibility(View.GONE);
            } else {
                viewHolder.lessonAddr.setText(bean.getJobAddress());
            }
            if (TextUtils.isEmpty(bean.getJobBeginTime())) {
                viewHolder.lessonStart.setVisibility(View.GONE);
            } else {
                viewHolder.lessonStart.setText(bean.getJobBeginTime());
            }

            viewHolder.lessonEnd.setText(bean.getJobEndTime());
            showState(viewHolder, bean);//控制显示按钮及文字
            return view;
        }


        private void showState(ViewHolder viewHolder, BuyLessonBean bean) {
            String remark = bean.getRemark();
            if (remark.equals(Constants.UNPAY)) {
                viewHolder.payState.setVisibility(View.GONE);
                viewHolder.cancel.setVisibility(View.VISIBLE);
                viewHolder.pay.setVisibility(View.VISIBLE);
                viewHolder.course.setVisibility(View.GONE);
                viewHolder.agreeRefund.setVisibility(View.GONE);
                viewHolder.refund.setVisibility(View.GONE);
            } else if (remark.equals(Constants.UNASSESS)) {
                viewHolder.payState.setVisibility(View.GONE);
                viewHolder.cancel.setVisibility(View.GONE);
                viewHolder.pay.setVisibility(View.GONE);
                viewHolder.course.setVisibility(View.VISIBLE);
                viewHolder.agreeRefund.setVisibility(View.GONE);
                viewHolder.refund.setVisibility(View.VISIBLE);
            } else if (remark.equals(Constants.REFUND)) {
                viewHolder.payState.setVisibility(View.VISIBLE);
                viewHolder.cancel.setVisibility(View.GONE);
                viewHolder.pay.setVisibility(View.GONE);
                viewHolder.course.setVisibility(View.VISIBLE);
                viewHolder.agreeRefund.setVisibility(View.GONE);
                viewHolder.refund.setVisibility(View.GONE);
            }
        }


    }

    /**
     * This class contains all butterknife-injected Views & Layouts from layout file 'item_buy_lesson.xml'
     * for easy to all layout elements.
     *
     * @author ButterKnifeZelezny, plugin for Android Studio by Avast Developers (http://github.com/avast)
     */
    static class ViewHolder {
        @InjectView(R.id.item_image)
        ImageView itemImage;
        @InjectView(R.id.lesson_name)
        TextView lessonName;
        @InjectView(R.id.lesson_start)
        TextView lessonStart;
        @InjectView(R.id.lesson_end)
        TextView lessonEnd;
        @InjectView(R.id.lesson_addr)
        TextView lessonAddr;
        @InjectView(R.id.delete)
        ImageView delete;
        @InjectView(R.id.payState)
        TextView payState;
        @InjectView(R.id.line)
        View line;
        @InjectView(R.id.agreeRefund)
        TextView agreeRefund;
        @InjectView(R.id.course)
        TextView course;
        @InjectView(R.id.pay)
        TextView pay;
        @InjectView(R.id.refund)
        TextView refund;
        @InjectView(R.id.cancel)
        TextView cancel;

        ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }

    class BuyLessonCommonTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProgressUtil.startProgressBar(getActivity());
            loading = true;
        }

        @Override
        protected String doInBackground(String... strings) {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("id", bean.getId() + "");
            BuyLessonCommonProtocol protocol = new BuyLessonCommonProtocol(hashMap);
            return protocol.load(strings[0], BaseProtocol.POST);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            ProgressUtil.stopProgressBar();
            loading = false;
            if (TextUtils.isEmpty(s)) {
                UIUtils.showToastSafe(R.string.network_error);
            } else {
                UIUtils.showToastSafe(s);
                refreshOrLoad();
                adapter.notifyDataSetChanged();
            }
        }
    }


}
