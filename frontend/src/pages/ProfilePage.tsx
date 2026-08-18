import { useEffect, useState, type FormEvent } from "react";
import axios from "axios";
import { api, extractErrorMessage } from "../api/client";
import type { StudentProfile } from "../api/types";

interface ProfileFormState {
  branch: string;
  graduationYear: string;
  cgpa: string;
  phone: string;
  resumeUrl: string;
}

const emptyForm: ProfileFormState = {
  branch: "",
  graduationYear: "",
  cgpa: "",
  phone: "",
  resumeUrl: "",
};

export function ProfilePage() {
  const [form, setForm] = useState<ProfileFormState>(emptyForm);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [hasExistingProfile, setHasExistingProfile] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadProfile() {
      try {
        const response = await api.get<StudentProfile>("/api/student/profile");
        if (cancelled) return;
        const profile = response.data;
        setForm({
          branch: profile.branch ?? "",
          graduationYear: profile.graduationYear?.toString() ?? "",
          cgpa: profile.cgpa?.toString() ?? "",
          phone: profile.phone ?? "",
          resumeUrl: profile.resumeUrl ?? "",
        });
        setHasExistingProfile(true);
      } catch (err) {
        if (axios.isAxiosError(err) && err.response?.status === 404) {
          setHasExistingProfile(false);
        } else {
          setError(extractErrorMessage(err));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void loadProfile();
    return () => {
      cancelled = true;
    };
  }, []);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccessMessage(null);
    setSubmitting(true);
    try {
      await api.put("/api/student/profile", {
        branch: form.branch || null,
        graduationYear: form.graduationYear ? Number(form.graduationYear) : null,
        cgpa: form.cgpa ? Number(form.cgpa) : null,
        phone: form.phone || null,
        resumeUrl: form.resumeUrl || null,
      });
      setHasExistingProfile(true);
      setSuccessMessage("Profile saved.");
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <p>Loading profile...</p>;
  }

  return (
    <div className="page-card">
      <h1>My Profile</h1>
      {!hasExistingProfile && (
        <p className="field-hint">
          You haven't set up your profile yet. Fill this in before applying to any drive.
        </p>
      )}
      <form onSubmit={handleSubmit} className="profile-form">
        <label>
          Branch
          <input
            type="text"
            value={form.branch}
            onChange={(e) => setForm({ ...form, branch: e.target.value })}
            placeholder="e.g. CSE"
          />
        </label>
        <label>
          Graduation year
          <input
            type="number"
            value={form.graduationYear}
            onChange={(e) => setForm({ ...form, graduationYear: e.target.value })}
            placeholder="e.g. 2027"
          />
        </label>
        <label>
          CGPA
          <input
            type="number"
            step="0.01"
            min="0"
            max="10"
            value={form.cgpa}
            onChange={(e) => setForm({ ...form, cgpa: e.target.value })}
          />
        </label>
        <label>
          Phone
          <input type="tel" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        </label>
        <label>
          Resume URL
          <input
            type="url"
            value={form.resumeUrl}
            onChange={(e) => setForm({ ...form, resumeUrl: e.target.value })}
            placeholder="https://..."
          />
        </label>
        {error && <p className="error-text">{error}</p>}
        {successMessage && <p className="success-text">{successMessage}</p>}
        <button type="submit" disabled={submitting}>
          {submitting ? "Saving..." : "Save profile"}
        </button>
      </form>
    </div>
  );
}
