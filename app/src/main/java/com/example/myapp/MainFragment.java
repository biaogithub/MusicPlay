package com.example.myapp;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment {

    private TextView left_textView;
    private TextView right_textView;


    private android.app.Fragment left_fragment;
    private android.app.Fragment right_fragment;
    private Fragment searchFragment,fragment;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private String which;
    public MainFragment() {
        // Required empty public constructor
    }

    public static Fragment newInstance(String which){
        MainFragment mainFragment=new MainFragment();
        Bundle bundle=new Bundle();
        bundle.putString("which",which);
        mainFragment.setArguments(bundle);
        return mainFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view=inflater.inflate(R.layout.fragment_main, container, false);
        left_textView = (TextView)view.findViewById(R.id.left_frag);
        right_textView = (TextView)view.findViewById(R.id.right_frag);
        left_textView.setTextColor(getResources().getColor(R.color.tab_color));
        right_textView.setTextColor(getResources().getColor(R.color.tab_color));
        left_textView.setOnClickListener(listener);
        right_textView.setOnClickListener(listener);
        Bundle bundle=getArguments();
        if(bundle!=null){
            this.which = bundle.getString("which");
            if(which!=null&&which.equals("right")){
                setFragment(1);
            }
        }else {
            setFragment(0);
        }
        view.setId(R.id.mainfragment);
        LinearLayout layout=(LinearLayout)view.findViewById(R.id.linear_search);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(searchFragment==null) {
                    searchFragment = new SearchFragment();
                }
                FragmentManager manager=getFragmentManager();
                FragmentTransaction transaction=manager.beginTransaction();
                transaction.replace(R.id.other_frag,searchFragment);
                transaction.commit();
            }
        });
        ImageButton menu=(ImageButton)view.findViewById(R.id.main_menu);
        drawerLayout=(DrawerLayout)getActivity().findViewById(R.id.my_drawer_layout);
        navigationView=(NavigationView)getActivity().findViewById(R.id.my_navigation);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(drawerLayout.isDrawerOpen(navigationView)){
                    drawerLayout.closeDrawer(navigationView);
                }else {
                    drawerLayout.openDrawer(navigationView);
                }
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    View.OnClickListener listener= new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.left_frag:
                    setFragment(0);

                    break;
                case R.id.right_frag:
                    setFragment(1);

                    break;
                default:
                    break;
            }
        }
    };
    private void setFragment(int index){
        FragmentManager manager=getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        switch (index){
            case 0:
                left_textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
                right_textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
                if(left_fragment == null){
                    left_fragment = new LeftFragment();
                    transaction.add(R.id.container,left_fragment);
                }
               hide(transaction);
//                transaction.replace(R.id.container,left_fragment);
                transaction.show(left_fragment);
                break;
            case 1:
                right_textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
                left_textView.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
                if(right_fragment ==null){
                    right_fragment = new RightFragment();
                    transaction.add(R.id.container,right_fragment);
                }
                hide(transaction);
                transaction.show(right_fragment);
//                transaction.replace(R.id.container,right_fragment);
                break;
            default:break;

        }
        transaction.commit();
    }
    private void hide(FragmentTransaction transaction){
        if(left_fragment!=null){
            transaction.hide(left_fragment);
        }
        if(right_fragment!=null){
            transaction.hide(right_fragment);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
