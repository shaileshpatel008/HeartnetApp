package in.dibc.kavitul.drcardio.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import in.dibc.kavitul.drcardio.ScanListFragment;

public class ScanPagerAdapter extends FragmentStateAdapter {
    public ScanPagerAdapter(FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return ScanListFragment.newInstance(position == 1); // true = reported
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
