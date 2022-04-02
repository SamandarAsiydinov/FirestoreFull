package uz.context.fullfirestore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import uz.context.fullfirestore.databinding.ActivityMainBinding
import uz.context.fullfirestore.model.Person
import uz.context.fullfirestore.utils.toast
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val personCollectionRef = Firebase.firestore.collection("persons")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()

    }

    private fun initViews() {
        binding.btnUploadData.setOnClickListener {
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val age = binding.etAge.text.toString().trim()
            if (isNotEmpty(firstName, lastName, age)) {
                binding.progressbar.isVisible = true
                val person = oldPerson()
                savePerson(person)
            } else {
                toast("Enter some data")
            }
        }
        //realtimeUpdates()
        binding.btnRetrieveData.setOnClickListener {
            getPersons()
        }
        binding.btnUpdatePerson.setOnClickListener {
            val oldPerson = oldPerson()
            val newPersonMap = getNewPersonMap()
            updatePerson(oldPerson, newPersonMap)
            toast("Data updated")
        }
        binding.btnDeleteData.setOnClickListener {
            val person = oldPerson()
            deletePerson(person)
            toast("Data deleted")
        }
    }

    private fun getNewPersonMap(): Map<String, Any> {
        val firstName = binding.etNewFirstName.text.toString()
        val lastName = binding.etNewLastName.text.toString()
        val age = binding.etNewAge.text.toString()
        val map = mutableMapOf<String, Any>()
        if (firstName.isNotEmpty()) {
            map["firstName"] = firstName
        }
        if (lastName.isNotEmpty()) {
            map["lastName"] = lastName
        }
        if (age.isNotEmpty()) {
            map["age"] = age.toInt()
        }
        return map
    }

    private fun oldPerson(): Person {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val age = binding.etAge.text.toString().toInt()
        return Person(firstName, lastName, age)
    }

    private fun deletePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionRef
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age)
            .get()
            .await()

        if (personQuery.documents.isNotEmpty()) {
            for (document in personQuery) {
                try {
                    personCollectionRef.document(document.id).delete().await()
//                    personCollectionRef.document(document.id).update(
//                        mapOf(
//                            "firstName" to FieldValue.delete()
//                        )
//                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        toast(e.message.toString())
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                toast("No Data")
            }
        }
    }

    private fun updatePerson(person: Person, newPersonMap: Map<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {
            val personQuery = personCollectionRef
                .whereEqualTo("firstName", person.firstName)
                .whereEqualTo("lastName", person.lastName)
                .whereEqualTo("age", person.age)
                .get()
                .await()

            if (personQuery.documents.isNotEmpty()) {
                for (document in personQuery) {
                    try {
                        personCollectionRef.document(document.id).set(
                            newPersonMap,
                            SetOptions.merge()
                        ).await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            toast(e.message.toString())
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    toast("No Data")
                }
            }
        }

    private fun realtimeUpdates() {
        personCollectionRef.addSnapshotListener { querySnapshot, fireStoreException ->
            fireStoreException?.let {
                toast(it.message.toString())
                return@addSnapshotListener
            }
            querySnapshot?.let {
                val sb = StringBuilder()

                for (document in it) {
                    val person = document.toObject<Person>()
                    sb.append("$person\n")
                }
                binding.tvPersons.text = sb.toString()
            }
        }
    }

    private fun getPersons() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val querySnapshot = personCollectionRef.get().await()
            val sb = StringBuilder()

            for (document in querySnapshot.documents) {
                val person = document.toObject<Person>()
                sb.append("$person\n")
            }

            withContext(Dispatchers.Main) {
                binding.tvPersons.text = sb.toString()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                toast(e.message.toString())
            }
        }
    }

    private fun saveToFirebase(person: Person) {
        personCollectionRef.add(person).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                toast("Successfully saved")
                binding.apply {
                    progressbar.isVisible = false
                    etAge.text.clear()
                    etFirstName.text.clear()
                    etLastName.text.clear()
                }
            } else {
                toast("Failed $task")
                Log.d("@@@@@", task.toString())
                binding.progressbar.isVisible = false
            }
        }
    }

    private fun savePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        try {
            personCollectionRef.add(person).await()
            withContext(Dispatchers.Main) {
                toast("Successfully saved data")
                binding.apply {
                    progressbar.isVisible = false
                    etAge.text.clear()
                    etFirstName.text.clear()
                    etLastName.text.clear()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                toast("Failed ${e.message}")
                binding.progressbar.isVisible = false
            }
        }
    }

    private fun isNotEmpty(s1: String, s2: String, s3: String): Boolean {
        return !(TextUtils.isEmpty(s1) || TextUtils.isEmpty(s2) || TextUtils.isEmpty(s3))
    }
}