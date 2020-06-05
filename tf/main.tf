provider "google" {
  project = var.project
  region  = var.region
  zone    = var.zone
}

resource "google_project_service" "bigquery_service" {
  project = var.project
  service = "bigquery.googleapis.com"

  disable_dependent_services = true
}

resource "google_bigquery_dataset" "test" {
  dataset_id                  = "test_data"
  friendly_name               = "test data dataset"
  description                 = "to test data"
  location                    = "US"
  default_table_expiration_ms = 86400000 * 10
}

resource "google_bigquery_dataset" "exports" {
  dataset_id                  = "exports"
  friendly_name               = "exports dataset"
  description                 = "To test exports"
  location                    = "US"
  default_table_expiration_ms = 86400000
  delete_contents_on_destroy  = true
}

resource "google_storage_bucket" "exports" {
  name          = "${var.project}-bq-exports"
  location      = "US"
  force_destroy = true

  lifecycle_rule {
    condition {
      age = "1"
    }
    action {
      type = "Delete"
    }
  }
}

resource "google_compute_instance" "process_launcher" {
  count        = var.create_launcher ? 1 : 0
  name         = "bq-export-propagator-launcher"
  machine_type = "n1-standard-1"
  zone         = var.zone

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-9"
    }
  }

  network_interface {
    network = "default"
    access_config {}
  }

  metadata_startup_script = file("${path.module}/startup_script.sh")

  service_account {
    email  = var.launcher_service_account
    scopes = ["userinfo-email", "compute-ro", "storage-ro", "bigquery", "logging-write", "monitoring"]
  }
}