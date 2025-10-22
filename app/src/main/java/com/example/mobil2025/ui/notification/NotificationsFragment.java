package com.example.mobil2025.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobil2025.R;
import com.example.mobil2025.data.repo.AllianceRepository;
import com.example.mobil2025.model.Alliance;
import com.example.mobil2025.model.AllianceInvitation;
import com.example.mobil2025.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private AllianceRepository allianceRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        notificationList = new ArrayList<>();
        allianceRepository = new AllianceRepository();
        adapter = new NotificationAdapter(notificationList, new NotificationAdapter.NotificationActionListener() {
            @Override
            public void onAccept(Notification notification) {
                handleAccept(notification);
            }

            @Override
            public void onReject(Notification notification) {
                handleReject(notification);
            }
        });
        rvNotifications.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadNotifications();
    }

    private void loadNotifications() {
        String currentUserId = auth.getCurrentUser().getUid();
        db.collection("notifications")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("read", false) // samo nepročitane
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Notification notification = doc.toObject(Notification.class);
                        notificationList.add(notification);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Greška pri učitavanju notifikacija", Toast.LENGTH_SHORT).show());
    }

    private void handleAccept(Notification notification) {
        String invitationId = notification.getRelatedInvitationId();
        db.collection("alliance_invitations").document(invitationId)
                .get()
                .addOnSuccessListener(doc -> {
                    AllianceInvitation invitation = doc.toObject(AllianceInvitation.class);
                    if (invitation != null) {
                        allianceRepository.acceptInvitation(invitation, new AllianceRepository.AllianceCallback() {
                            @Override
                            public void onSuccess(Alliance alliance) {
                                markAsRead(notification); // oznaci notifikaciju kao procitanu
                            }
                            @Override
                            public void onRejected(AllianceInvitation invitation) {}
                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    private void handleReject(Notification notification) {
        String invitationId = notification.getRelatedInvitationId();
        db.collection("alliance_invitations").document(invitationId)
                .get()
                .addOnSuccessListener(doc -> {
                    AllianceInvitation invitation = doc.toObject(AllianceInvitation.class);
                    if (invitation != null) {
                        allianceRepository.rejectInvitation(invitation, new AllianceRepository.AllianceCallback() {
                            @Override
                            public void onSuccess(Alliance alliance) {}
                            @Override
                            public void onRejected(AllianceInvitation invitation) {
                                markAsRead(notification); // oznaci notifikaciju kao procitanu
                            }
                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(getContext(), "Greška: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }


    private void markAsRead(Notification notification) {
        notification.setRead(true);
        db.collection("notifications")
                .document(notification.getId())
                .set(notification)
                .addOnSuccessListener(aVoid -> adapter.notifyDataSetChanged());
    }
}
