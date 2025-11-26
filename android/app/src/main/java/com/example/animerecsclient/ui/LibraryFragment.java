package com.example.animerecsclient.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.example.animerecsclient.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LibraryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);

        // Налаштування адаптера сторінок
        viewPager.setAdapter(new LibraryPagerAdapter(this));

        // Зв'язуємо TabLayout і ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Liked"); break;
                case 1: tab.setText("Planned"); break;
                case 2: tab.setText("Disliked"); break;
            }
        }).attach();
    }

    // Внутрішній адаптер для вкладок
    private static class LibraryPagerAdapter extends FragmentStateAdapter {
        public LibraryPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return LibraryListFragment.newInstance("LIKE");
                case 1: return LibraryListFragment.newInstance("PLAN");
                default: return LibraryListFragment.newInstance("DISLIKE");
            }
        }

        @Override
        public int getItemCount() {
            return 3; // Три вкладки
        }
    }
}