package com.example.fimae.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;

import com.example.fimae.R;
import com.example.fimae.models.DisableUser;
import com.example.fimae.repository.AuthRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.example.fimae.models.Fimaers;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;


public class AuthenticationActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword, editTextRepeatPass;
    TextInputLayout repeatPassLayout, passLayout;

    Button btnSignIn;

    TextView signUpTextView, forgotPasswordTextView;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    AuthRepository authRepository = AuthRepository.getInstance();
    ProgressBar progressBar;
    ConstraintLayout contentLayout;
    GoogleSignInClient mGoogleSignInClient;
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    CollectionReference fimaeUsersRefer = firestore.collection("fimae-users");
    DatabaseReference usersRef = database.getReference("fimae-users");
    boolean isRegister = false, isForgotPass = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        setContentView(R.layout.activity_authentication);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextRepeatPass = findViewById(R.id.editTextRepeatPassword);
        repeatPassLayout = findViewById(R.id.repeatPassInput);
        passLayout = findViewById(R.id.pass_input_layout);
        signUpTextView = findViewById(R.id.textViewRegister);
        btnSignIn = findViewById(R.id.buttonLogin);
        forgotPasswordTextView = findViewById(R.id.textViewForgotPassword);
        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.content_layout);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);



        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //signUp();
                if (!isRegister) {
                    repeatPassLayout.setVisibility(View.VISIBLE);
                    editTextPassword.setVisibility(View.VISIBLE);
                    passLayout.setVisibility(View.VISIBLE);
                    btnSignIn.setText("Register");
                    signUpTextView.setText("Has an account? Login here.");
                    forgotPasswordTextView.setVisibility(View.VISIBLE);
                    forgotPasswordTextView.setText("Forgot password? Reset here.");
                    isRegister = true;
                    isForgotPass = false;
                } else {
                    repeatPassLayout.setVisibility(View.GONE);
                    btnSignIn.setText("Login");
                    signUpTextView.setText("New user? Register Now");
                    isRegister = false;
                    forgotPasswordTextView.setVisibility(View.VISIBLE);
                    isForgotPass = false;
                }

            }
        });
        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isForgotPass) {
                    repeatPassLayout.setVisibility(View.GONE);
                    forgotPasswordTextView.setText("Has an account? Login here.");
                    editTextPassword.setVisibility(View.GONE);
                    passLayout.setVisibility(View.GONE);
                    btnSignIn.setText("RESET PASSWORD");
                    signUpTextView.setVisibility(View.VISIBLE);
                    signUpTextView.setText("New user? Register Now!");
                    isForgotPass = true;
                    isRegister = false;
                } else {
                    repeatPassLayout.setVisibility(View.GONE);

                    signUpTextView.setVisibility(View.VISIBLE);
                    editTextPassword.setVisibility(View.VISIBLE);
                    forgotPasswordTextView.setText("Forgot password? Reset here.");

                    signUpTextView.setText("New user? Register Now!");
                    passLayout.setVisibility(View.VISIBLE);
                    btnSignIn.setText("Login");

                    isForgotPass = false;
                    isRegister = false;
                }

            }
        });
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRegister) {
                    signUp();
                } else if (isForgotPass) {
                    resetPass();
                } else {
                    signIn();
                }
            }
        });


        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            successAuthentication();
        }
    }

    int RC_SIGN_IN = 9001;

    private void signInWithGoogle() {
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    void successAuthentication() {

        Intent intent = new Intent(AuthenticationActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    void navToUpdateProfile() {
        Intent intent = new Intent(AuthenticationActivity.this, CreateProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void signIn() {
        String email = String.valueOf(editTextEmail.getText());
        String password = String.valueOf(editTextPassword.getText());
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);
        authRepository.signIn(email, password, new AuthRepository.SignInCallback() {
            @Override
            public void onSignInSuccess(FirebaseUser user) {
                FirebaseFirestore.getInstance().collection("user_disable")
                        .document(user.getUid() + "USER")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot documentSnapshot = task.getResult();
                                    if (documentSnapshot != null) {
                                        DisableUser disableUser = documentSnapshot.toObject(DisableUser.class);
                                        if (disableUser != null && disableUser.getTimeEnd().after(new Date()) && disableUser.getType() != null && disableUser.getType().equals("USER")) {
                                            Intent intent = new Intent(AuthenticationActivity.this, DisabledUserActivity.class);
                                            intent.putExtra("disableId", user.getUid());
                                            intent.putExtra("type", "USER");
                                            startActivity(intent);
                                        }
                                    }
                                }
                                // Call successAuthentication() here, as this will be triggered after the get() operation completes, whether it succeeds or fails.
                                successAuthentication();
                            }
                        });
            }

            @Override
            public void onSignInError(String errorMessage) {
                editTextEmail.setError(errorMessage);
                Toast.makeText(getApplicationContext(), "Email hoặc mật khẩu không đúng", Toast.LENGTH_SHORT).show();
            }
        });
        progressBar.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);

    }

    private void resetPass() {
        String email = editTextEmail.getText().toString();

        try{
            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(AuthenticationActivity.this, "Đã gửi email đặt lại mật khẩu đến " + email, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(AuthenticationActivity.this, "Đặt lại mật khẩu thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), "Lỗi rồi", Toast.LENGTH_SHORT).show();
        }



    }

    private void signUp() {
        String email = String.valueOf(editTextEmail.getText());
        String password = String.valueOf(editTextPassword.getText());
        String repeatPass = String.valueOf(editTextRepeatPass.getText());
        if (!(email.isEmpty() && password.isEmpty() && repeatPass.isEmpty())) {
            if (!repeatPass.equals(password)) {
                editTextRepeatPass.setError("Vui lòng nhập lại mật khẩu giống phía trên");
                return;
            }
            authRepository.signUp(email, password, new AuthRepository.SignUpCallback() {
                @Override
                public void onSignUpSuccess(FirebaseUser user) {
                    navToUpdateProfile();
                }

                @Override
                public void onSignUpError(String errorMessage) {
                    editTextEmail.setError(errorMessage);
                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "Nhập tk mk đi bạn êi!",
                    Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == 0) {

            }
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnCompleteListener(new OnCompleteListener<GoogleSignInAccount>() {
                        @Override
                        public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                            if (task.isSuccessful()) {
                                try {
                                    GoogleSignInAccount account = task.getResult(ApiException.class);
                                    authRepository.googleAuth(account.getIdToken(), new AuthRepository.GoogleSignInCallback() {
                                        @Override
                                        public void onSignInSuccess(FirebaseUser user) {
                                            successAuthentication();
                                        }

                                        @Override
                                        public void onFirstTimeSignIn(FirebaseUser user) {
                                            navToUpdateProfile();
                                        }

                                        @Override
                                        public void onSignInError(String errorMessage) {
                                            Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } catch (ApiException e) {
                                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                                }
                            }
                        }
                    });

        }
    }


}
