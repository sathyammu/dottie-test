RG=rg-brimma-dev
KV_NAME=kv-brimma-dev
CERT_NAME=brimma-wq-non-prod-cert
SUBJ=CN=brimmatech.com
KEY_NAME=jwt-signing-key
az deployment group create \
  --resource-group $RG \
  --parameters keyVaultName=$KV_NAME \
  --parameters certificateName=$CERT_NAME \
  --parameters subjectName=$SUBJ \
  --template-file public-key-as-pem.bicep



#az keyvault secret download \
#  --vault-name <your-kv-name> \
#  --name <certificateName> \
#  --encoding base64 \
#  --file cert.pem

az deployment group show \
  --resource-group $RG \
  --name public-key-as-pem \
  --query "properties.error.details" \
  -o json

  az deployment group list \
    --resource-group $RG \
    --query "[].{Name:name, Timestamp:properties.timestamp}" \
    -o table

    az keyvault show \
      --name $KV_NAME \
      --query "sku.name" -o tsv

{
  "issuerParameters": {
    "certificateTransparency": null,
    "name": "Self"
  },
  "keyProperties": {
    "curve": null,
    "exportable": true,
    "keySize": 2048,
    "keyType": "RSA",
    "reuseKey": true
  },
  "lifetimeActions": [
    {
      "action": {
        "actionType": "AutoRenew"
      },
      "trigger": {
        "daysBeforeExpiry": 90
      }
    }
  ],
  "secretProperties": {
    "contentType": "application/x-pem-file"
  },
  "x509CertificateProperties": {
    "keyUsage": [
      "cRLSign",
      "dataEncipherment",
      "digitalSignature",
      "keyEncipherment",
      "keyAgreement",
      "keyCertSign"
    ],
    "subject": "CN=brimmatech.com",
    "validityInMonths": 12
  }
}

openssl x509 -in cert.pem -text --noout
openssl x509 -in cert.pem -text

az keyvault certificate create \
  --vault-name $KV_NAME \
  --name $CERT_NAME \
  --policy "$(az keyvault certificate get-default-policy \
               | jq '.x509CertificateProperties.subject="CN=brimmatech.com"' \
               | jq '.keyProperties.keySize=2048' \
               | jq '.keyProperties.keyType="RSA"' \
               | jq '.secretProperties.contentType="application/x-pem-file"')"

az keyvault certificate create \
  --vault-name $KV_NAME \
  --name $CERT_NAME \
  --policy "$(az keyvault certificate get-default-policy \
               | jq '.x509CertificateProperties.subject="CN=brimmatech.com"' \
              | jq '.issuerParameters.name="Unknown"' \
               | jq '.keyProperties.keySize=2048' \
               | jq '.keyProperties.reuseKey=true' \
               | jq '.lifetimeActions[0].action.actionType="EmailContacts"' \
               | jq '.secretProperties.contentType="application/x-pem-file"')"

az keyvault certificate download \
  --vault-name $KV_NAME \
  --name $CERT_NAME \
  --encoding PEM \
  --file cert.pem

az keyvault key download --vault-name $KV_NAME --name $KEY_NAME  --file public-signing.pem

openssl x509 -in cert.pem -pubkey -noout > cert-public.pem
openssl rsa -pubin -in cert-public.pem -modulus -noout
openssl rsa -pubin -in public-signing.pem -modulus -noout

az keyvault key show --vault-name $KV_NAME --name $KEY_NAME --query "key.n"

openssl x509 -in cert.pem -outform DER -out cert.der

az keyvault certificate pending merge \
  --vault-name $KV_NAME \
  --name $CERT_NAME \
  --file cert.der

  openssl x509 -in cert.pem -noout -fingerprint


import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.FileInputStream;
import javax.xml.bind.DatatypeConverter;

public static String getThumbprint(Path pemPath) throws Exception {
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    X509Certificate cert = (X509Certificate) cf.generateCertificate(
        new FileInputStream(pemPath.toFile())
    );
    byte[] der = cert.getEncoded();
    MessageDigest md = MessageDigest.getInstance("SHA-1");            // or "SHA-256"
    byte[] digest = md.digest(der);
    String hex = DatatypeConverter.printHexBinary(digest);
    // if you want with colons:
    // return hex.replaceAll("(?<=..)(..)", ":$1");
    return hex;
}
Use the resulting hex string as your JWT header’s kid when you upload the cert to Auth0.


https://chatgpt.com/c/68522a9b-3330-8002-9ca9-825a445b3028

Here’s a Java test program to verify if your JWT signed client assertion is valid against a certificate (typically a public key cert), and whether it could be used in a typical OAuth 2.0 Client Credentials flow using client_assertion.

✅ Prerequisites:
JWT is signed using the private key corresponding to the cert’s public key.

JWT includes correct claims: iss, sub, aud, exp, jti.

🔧 Test Program (Java, using Nimbus JOSE + JWT)
java
Copy
Edit
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;

import java.io.FileInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;

public class JwtAssertionVerifier {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java JwtAssertionVerifier <cert.pem> <jwt-token>");
            System.exit(1);
        }

        String certPath = args[0];
        String jwtString = args[1];

        // Load public key from certificate
        X509Certificate cert = loadCertificate(certPath);
        RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();

        // Parse JWT
        SignedJWT signedJWT = SignedJWT.parse(jwtString);

        // Verify signature
        boolean isSignatureValid = signedJWT.verify(new RSASSAVerifier(publicKey));
        System.out.println("Signature valid: " + isSignatureValid);

        // Check claims
        Date now = new Date();
        Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();

        boolean notExpired = exp != null && exp.after(now);
        String aud = signedJWT.getJWTClaimsSet().getAudience().toString();
        String iss = signedJWT.getJWTClaimsSet().getIssuer();
        String sub = signedJWT.getJWTClaimsSet().getSubject();

        System.out.println("JWT expiration valid: " + notExpired);
        System.out.println("Audience (aud): " + aud);
        System.out.println("Issuer (iss): " + iss);
        System.out.println("Subject (sub): " + sub);

        if (isSignatureValid && notExpired) {
            System.out.println("✅ JWT assertion appears valid for OAuth client_credentials flow.");
        } else {
            System.out.println("❌ JWT assertion is not valid.");
        }
    }

    private static X509Certificate loadCertificate(String path) throws Exception {
        try (FileInputStream fis = new FileInputStream(path)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(fis);
        }
    }
}
🧪 Compile and Run:
bash
Copy
Edit
javac -cp .:nimbus-jose-jwt-9.31.jar JwtAssertionVerifier.java
java -cp .:nimbus-jose-jwt-9.31.jar JwtAssertionVerifier cert.pem "eyJhbGciOiJSUzI1NiIsInR5cCI6..."
🔍 Replace cert.pem and JWT string with your actual cert file and client assertion.