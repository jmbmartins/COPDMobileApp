package com.example.copdmonitorapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RegisterPage extends AppCompatActivity {

    TextView TextVClickableGoLogin;

    TextInputEditText editTextEmail;
    TextInputEditText editTextPassword;
    TextInputEditText editTextPasswordRepeat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_page);
        TextVClickableGoLogin = findViewById(R.id.txtViewGoLogin);
        editTextEmail = findViewById(R.id.eTxtEmail);
        editTextPassword = findViewById(R.id.eTxtPassword);
        editTextPasswordRepeat = findViewById(R.id.eTxtPasswordRepeat);
    }

    public void onRegisterClicked(View view) {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String passwordRepeat = editTextPasswordRepeat.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || passwordRepeat.isEmpty()) {
            // Empty fields
            Toast.makeText(RegisterPage.this, "Hey there! It seems like you left some fields empty!", Toast.LENGTH_LONG).show();
            return;
        }

        if (!password.equals(passwordRepeat)) {
            // Passwords don't match
            Toast.makeText(RegisterPage.this, "It seems like the passwords you entered don't match!", Toast.LENGTH_LONG).show();
            return;
        }

        new UserRegistrationTask().execute(email, password);
    }

    private class UserRegistrationTask extends AsyncTask<String, Void, Boolean> {
        private Exception exception;

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(RegisterPage.this);
            progressDialog.setMessage("Processing, please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String email = params[0];
            String password = params[1];
            String salt = BCrypt.gensalt();
            String hashedPassword = BCrypt.hashpw(password, salt);

            System.out.println("Email " + email + "\n Pass " + password +  "\n Salt " +  salt +  "\n Hashed " + hashedPassword);

            String svurl = "jdbc:postgresql://copd-db-instance.cr6kvihylkhm.eu-north-1.rds.amazonaws.com:5432/copd_db";
            String svurl = "jdbc:postgresql://nothing2lose-db.carkfyqrpaoi.eu-north-1.rds.amazonaws.com:5432/NOTHING2LOSEDB";
            String svusername = "postgres";
            String svpassword = "copdproject";


            try (Connection conn = DriverManager.getConnection(svurl, svusername, svpassword)) {
                // Check if there is another user with the same username
                String selectQuery = "SELECT COUNT(*) FROM Pacient WHERE email = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(selectQuery)) {
                    pstmt.setString(1, email);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        rs.next();
                        int count = rs.getInt(1);
                        if (count == 0) {
                            // User does not exist, insert into database
                            String insertQuery = "INSERT INTO Pacient (email, password, salt) VALUES (?, ?, ?)";
                            try (PreparedStatement pstmt2 = conn.prepareStatement(insertQuery)) {
                                pstmt2.setString(1, email);
                                pstmt2.setString(2, hashedPassword);
                                pstmt2.setString(3, salt);
                                pstmt2.executeUpdate();
                            }
                            return true;
                        }
                        return false;
                    }
                }
            } catch (Exception e) {
                Log.e("MyApp", "Error executing query", e);
                exception = e;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            progressDialog.dismiss();

            if (result) {
                Toast.makeText(getApplicationContext(), "Successful registration!", Toast.LENGTH_SHORT).show();
                saveUserInSharedPreferences();
                goToHomePage();
            } else {
                Toast.makeText(RegisterPage.this, "Sorry, the email is already in use!", Toast.LENGTH_LONG).show();
            }
        }
    }


    public void goToHomePage(){
        Intent intent = new Intent(this, InitialPage.class);
        startActivity(intent);
    }

    public void onLoginLinkClicked(View view){
        Intent intent = new Intent(this, InitialPage.class);
        startActivity(intent);
    }

    public void saveUserInSharedPreferences(){
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("email", editTextEmail.getText().toString());
        editor.putString("password", editTextPassword.getText().toString());
        editor.commit();
    }

}
