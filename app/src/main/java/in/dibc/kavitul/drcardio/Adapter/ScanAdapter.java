package in.dibc.kavitul.drcardio.Adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import in.dibc.kavitul.drcardio.Model.ScanModel;
import in.dibc.kavitul.drcardio.R;
import in.dibc.kavitul.drcardio.utils.DateConverter;

public class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.ScanViewHolder> {

    public interface OnScanClickListener {
        void onScanClick(ScanModel model, int position);
    }

    List<ScanModel> scanList = new ArrayList<>();
    private OnScanClickListener listener;

    public void setScans(List<ScanModel> scans, OnScanClickListener listener) {
        scanList = scans;
        this.listener = listener;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ScanViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_scan, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ScanViewHolder holder, int position) {
        ScanModel scan = scanList.get(position);
        String abnotmal = "Yes";
        if(scan.isAbnormal()) {
            abnotmal = "Yes";
        } else {
            abnotmal = "No";
        }
        holder.name.setText("Patient Name : "+scan.getPatientName());
        holder.tvInstitutionName.setText("Institution Name : "+scan.getInstitutionName());
        holder.tvUploadedBy.setText("Uploaded By : "+scan.getUploadUserName());
        holder.tvStatus.setText("Abnormal : "+abnotmal);
        holder.date.setText(DateConverter.formatDateTime(scan.getStudyDate()));

        holder.btnViewScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) {
                    listener.onScanClick(scan, position);
                }
            }
        });


//        Glide.with(holder.image.getContext()).load(scan.getImageUrl()).into(holder.image);
    }

    @Override
    public int getItemCount() {
        return scanList.size();
    }

    static class ScanViewHolder extends RecyclerView.ViewHolder {
        TextView name, date, tvUploadedBy, tvInstitutionName, tvStatus;
        ImageView image;
        Button btnViewScan;

        public ScanViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvName);
            date = itemView.findViewById(R.id.tvDate);
            tvUploadedBy = itemView.findViewById(R.id.tvUploadedBy);
            tvInstitutionName = itemView.findViewById(R.id.tvInstitutionName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            image = itemView.findViewById(R.id.imgScan);
            btnViewScan = itemView.findViewById(R.id.btnViewScan);
        }
    }

    public void addScans(List<ScanModel> newScans) {
        int startPosition = this.scanList.size();
        this.scanList.addAll(newScans);
        notifyItemRangeInserted(startPosition, newScans.size());
    }
}