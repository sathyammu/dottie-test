from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
import base64
import json

# Paste your `n` and `e` values from Azure here (base64url format)
n_b64url = '6YgHy4PlFkGTH87luwkfS5k1DwNkCdydAntNQi5xqvVvc4sFF2IDafBOfoj0kTygAYuZiL3lBEMeZKPAZ/Y31z+wxHzfNNIbwShkFMApk7PnmwiGZXCuI+sZQQQJW0uLGSoUjbbLJOoOywQMXYKtMlUMGFJVy5cClCyHnjAaPODMNTOMSojoAjzcE+A9DV4Q2CYbgJpMbUGKTQl+iqHTckQT+C63lLreEJDLBxUrRLO6c97X4Xf6PAnUe/HaubBJfZRi8ExcD8epf+BR8y6+I/aCnRslY79IbvRNOeO71J7igkAuhoCa3cu4u5ZG8qY+jLgzXW+IbBR6Q7wbC5nKGQ=='
e_b64url = 'AQAB'

# Decode from base64url
def b64url_decode(data):
    padding = '=' * ((4 - len(data) % 4) % 4)
    return base64.urlsafe_b64decode(data + padding)

n_bytes = b64url_decode(n_b64url)
e_bytes = b64url_decode(e_b64url)

# Convert bytes to int
n = int.from_bytes(n_bytes, 'big')
e = int.from_bytes(e_bytes, 'big')

# Construct public key
public_numbers = rsa.RSAPublicNumbers(e, n)
public_key = public_numbers.public_key()

# Export to PEM
pem = public_key.public_bytes(
    encoding=serialization.Encoding.PEM,
    format=serialization.PublicFormat.SubjectPublicKeyInfo
)

print(pem.decode())