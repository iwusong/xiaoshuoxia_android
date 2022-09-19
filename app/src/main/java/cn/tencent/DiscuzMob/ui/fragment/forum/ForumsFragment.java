package cn.tencent.DiscuzMob.ui.fragment.forum;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import cn.tencent.DiscuzMob.base.RedNet;
import cn.tencent.DiscuzMob.base.RedNetApp;
import cn.tencent.DiscuzMob.model.AllForumBean;
import cn.tencent.DiscuzMob.model.CatlistBean;
import cn.tencent.DiscuzMob.model.MyFavForumBean;
import cn.tencent.DiscuzMob.net.AppNetConfig;
import cn.tencent.DiscuzMob.ui.Event.ReFreshImg;
import cn.tencent.DiscuzMob.ui.adapter.ForumsAdapter;
import cn.tencent.DiscuzMob.ui.adapter.MyBaseExpandableListAdapter;
import cn.tencent.DiscuzMob.ui.fragment.SimpleRefreshFragment;
import cn.tencent.DiscuzMob.utils.cache.CacheUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/**
 * Created by cg on 2017/4/14.
 */

public class ForumsFragment extends SimpleRefreshFragment implements SwipeRefreshLayout.OnRefreshListener {
    //    private XExpandableListView xExpandableListView;
    private ListView listview;
    //一级列表的集合
    private List<CatlistBean> listGroup;
    //二级列表的集合
    private List<List<String>> listChild;
    private MyBaseExpandableListAdapter myAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRefreshView.setOnRefreshListener(this);
        mTip.setDisplayedChild(1);
        onRefresh();
    }

    String name;
    String posts;
    String todayposts;
    ForumsAdapter forumsAdapter;

    private void getDataFromNe() {
        String cookiepre = CacheUtils.getString(RedNetApp.getInstance(), "cookiepre");
        Log.e("TAG", "cookiepre2=" + cookiepre);
        String cookiepre_auth = CacheUtils.getString(RedNetApp.getInstance(), "cookiepre_auth");
        String cookiepre_saltkey = CacheUtils.getString(RedNetApp.getInstance(), "cookiepre_saltkey");
        RedNet.mHttpClient.newCall(new Request.Builder()
                        .url(AppNetConfig.ALLFORUM)
                        .cacheControl(new CacheControl.Builder().noStore().noCache().build()).build())
                .enqueue(new com.squareup.okhttp.Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        getActivity().runOnUiThread(() -> {
                            mRefreshView.setRefreshing(false);
                            mTip.setDisplayedChild(0);
                            Toast.makeText(RedNetApp.getInstance(), "网络请求失败", Toast.LENGTH_SHORT).show();
                        });

                    }

                    @Override
                    public void onResponse(Response res) {

                        RedNet.mHttpClient.newCall(new Request.Builder()
                                        .url(AppNetConfig.MYFAV)
                                        .cacheControl(new CacheControl.Builder().noStore().noCache().build()).build())
                                .enqueue(new Callback() {

                                    @Override
                                    public void onFailure(Request requestfv, IOException efv) {

                                        String response = null;
                                        try {
                                            response = res.body().string();
                                            String finalResponse = response;
                                            getActivity().runOnUiThread(() -> {
                                                mRefreshView.setRefreshing(false);
                                                mTip.setDisplayedChild(0);
                                                AllForumBean allForumBean = null;
                                                if (finalResponse != null && !TextUtils.isEmpty(finalResponse) && !finalResponse.contains("error")) {
                                                    try {
                                                        if (mRefreshView != null) {
                                                            allForumBean = new Gson().fromJson(finalResponse, AllForumBean.class);
                                                            listGroup = allForumBean.getVariables().getCatlist();
                                                            listChild = new ArrayList<List<String>>();
                                                            CacheUtils.putString(RedNetApp.getInstance(), "formhash2", allForumBean.getVariables().getFormhash());
                                                            CacheUtils.putString(RedNetApp.getInstance(), "cookiepre2", allForumBean.getVariables().getCookiepre());
                                                            if (listGroup != null && listGroup.size() > 0) {
                                                                imageView.setVisibility(View.GONE);
                                                                for (int i = 0; i < listGroup.size(); i++) {
                                                                    List<String> forums = listGroup.get(i).getForums();
                                                                    listChild.add(forums);
                                                                }
                                                                final List<AllForumBean.VariablesBean.ForumlistBean> forumlist = allForumBean.getVariables().getForumlist();
                                                                forumsAdapter = new ForumsAdapter(getActivity(), listGroup, listChild, forumlist);
                                                                mListView.setAdapter(forumsAdapter);
                                                            } else {
                                                                imageView.setVisibility(View.VISIBLE);
                                                            }


                                                        } else {
                                                            onRefresh();
                                                        }
                                                    } catch (JsonSyntaxException e) {
                                                        e.printStackTrace();
                                                    }
                                                } else {
                                                    Toast.makeText(RedNetApp.getInstance(), "请求失败", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } catch (IOException e) {
                                            Toast.makeText(RedNetApp.getInstance(), "加载已收藏失败", Toast.LENGTH_SHORT).show();

                                        }

                                    }

                                    @Override
                                    public void onResponse(Response responsefv) throws IOException {
                                        String response = res.body().string();
                                        String string = responsefv.body().string();
                                        MyFavForumBean myFavForumBean = new Gson().fromJson(string, MyFavForumBean.class);
                                        List<MyFavForumBean.VariablesBean.ListBean> list = myFavForumBean.getVariables().getList();
                                        List<String> ls = new ArrayList<>();
                                        for (MyFavForumBean.VariablesBean.ListBean listBean : list) {

                                            ls.add(listBean.getId());
                                        }
                                        CatlistBean catlistBean = new CatlistBean();
                                        catlistBean.setFid("xxsfav");
                                        catlistBean.setName("已收藏");
                                        catlistBean.setForums(ls);
                                        getActivity().runOnUiThread(() -> {
                                            mRefreshView.setRefreshing(false);
                                            mTip.setDisplayedChild(0);
                                            AllForumBean allForumBean = null;
                                            if (response != null && !TextUtils.isEmpty(response) && !response.contains("error")) {
                                                try {
                                                    if (mRefreshView != null) {
                                                        allForumBean = new Gson().fromJson(response, AllForumBean.class);
                                                        listGroup = allForumBean.getVariables().getCatlist();
                                                        listGroup.add(0, catlistBean);
                                                        listChild = new ArrayList<List<String>>();
                                                        CacheUtils.putString(RedNetApp.getInstance(), "formhash2", allForumBean.getVariables().getFormhash());
                                                        CacheUtils.putString(RedNetApp.getInstance(), "cookiepre2", allForumBean.getVariables().getCookiepre());
                                                        if (listGroup != null && listGroup.size() > 0) {
                                                            imageView.setVisibility(View.GONE);
                                                            for (int i = 0; i < listGroup.size(); i++) {
                                                                List<String> forums = listGroup.get(i).getForums();
                                                                listChild.add(forums);
                                                            }
                                                            final List<AllForumBean.VariablesBean.ForumlistBean> forumlist = allForumBean.getVariables().getForumlist();
                                                            forumsAdapter = new ForumsAdapter(getActivity(), listGroup, listChild, forumlist);
                                                            mListView.setAdapter(forumsAdapter);
                                                        } else {
                                                            imageView.setVisibility(View.VISIBLE);
                                                        }


                                                    } else {
                                                        onRefresh();
                                                    }
                                                } catch (JsonSyntaxException e) {
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                Toast.makeText(RedNetApp.getInstance(), "请求失败", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                });
                    }
                });
    }

    @Override
    public void onRefresh() {
        if (forumsAdapter != null) {
            forumsAdapter.cleanData();
        }
        getDataFromNe();
    }

    @Subscribe
    public void onEventMainThread(ReFreshImg img) {
        Log.e("TAG", "无图模式");
        onRefresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
