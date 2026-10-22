package in.dibc.kavitul.drcardio.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import in.dibc.kavitul.drcardio.Model.Patient;
import in.dibc.kavitul.drcardio.R;
import in.dibc.kavitul.drcardio.utils.DateConverter;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.ViewHolder> {
    private List<Patient> userList;
    private OnItemClickListener onItemClickListener;

    // Interface for Click Events
    public interface OnItemClickListener {
        void onItemClick(Patient patient);
    }

    public PatientAdapter(List<Patient> userList, OnItemClickListener listener) {
        this.userList = userList;
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_patient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Patient user = userList.get(position);
        holder.tvName.setText(user.getPatient_Fname() + " " + user.getPatient_Mname() + " " + user.getPatient_Lname());
        holder.tvMobile.setText("Mobile: " + user.getPatient_Mobile1());
        holder.tvDob.setText("DOB: " + DateConverter.formatDate(user.getPatient_DOB()));

        // Handle Click Event
        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMobile, tvDob;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvMobile = itemView.findViewById(R.id.tvMobile);
            tvDob = itemView.findViewById(R.id.tvDob);
        }
    }
}

