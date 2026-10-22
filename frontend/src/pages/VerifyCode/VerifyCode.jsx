import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { authService } from "../../services/auth/authService";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import {
  EMAIL_VERIFICATION_KEY,
  CODE_TYPE_KEY,
  CODE_TYPE_REGISTER,
  CODE_TYPE_RESET_PASSWORD,
  OTP_LENGTH,
} from "../../constants";
import { storage } from "../../utils";
import AuthLayout, { AuthError, AuthSubmit } from "../../components/AuthLayout/AuthLayout";
import style from "../../components/AuthLayout/AuthLayout.module.css";

export default function VerifyCode() {
  const [digits, setDigits] = useState(Array(OTP_LENGTH).fill(""));
  const [email, setEmail] = useState("");
  const [codeType, setCodeType] = useState(CODE_TYPE_REGISTER);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const inputsRef = useRef([]);
  const navigate = useNavigate();
  const { login } = useAuth();
  const { success: notifySuccess, error: notifyError } = useToast();

  useEffect(() => {
    const storedEmail = storage.get(EMAIL_VERIFICATION_KEY);
    const storedCodeType = storage.get(CODE_TYPE_KEY) ?? CODE_TYPE_REGISTER;
    if (!storedEmail) {
      navigate("/register");
    } else {
      setEmail(storedEmail);
      setCodeType(storedCodeType);
    }
  }, [navigate]);

  function handleChange(index, value) {
    const char = value.replace(/\D/g, "").slice(-1);
    const next = [...digits];
    next[index] = char;
    setDigits(next);
    if (char && index < OTP_LENGTH - 1) {
      inputsRef.current[index + 1].focus();
    }
  }

  function handleKeyDown(index, e) {
    if (e.key === "Backspace") {
      if (digits[index]) {
        const next = [...digits];
        next[index] = "";
        setDigits(next);
      } else if (index > 0) {
        inputsRef.current[index - 1].focus();
      }
    }
  }

  function handlePaste(e) {
    e.preventDefault();
    const pasted = e.clipboardData.getData("text").replace(/\D/g, "").slice(0, OTP_LENGTH);
    const next = Array(OTP_LENGTH).fill("");
    pasted.split("").forEach((char, i) => { next[i] = char; });
    setDigits(next);
    const focusIndex = Math.min(pasted.length, OTP_LENGTH - 1);
    inputsRef.current[focusIndex].focus();
  }

  const handleResend = async () => {
    setResending(true);
    try {
      if (codeType === CODE_TYPE_RESET_PASSWORD) {
        await authService.resendPasswordCode(email);
      } else {
        await authService.resendCode(email);
      }
      setDigits(Array(OTP_LENGTH).fill(""));
      notifySuccess("Novo código enviado para o seu e-mail.");
      inputsRef.current[0]?.focus();
    } catch {
      notifyError("Não foi possível reenviar o código.");
    } finally {
      setResending(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const code = digits.join("");
    if (code.length < OTP_LENGTH) return;

    setError("");
    setLoading(true);
    try {
      const { data } = await authService.verify({ email, codeType, code });

      storage.remove(EMAIL_VERIFICATION_KEY);
      storage.remove(CODE_TYPE_KEY);

      if (codeType === CODE_TYPE_RESET_PASSWORD) {
        login(data);
        navigate("/reset-password", { replace: true });
      } else {
        login(data);
        navigate("/", { replace: true });
      }
    } catch (err) {
      setError(err.response?.data?.message ?? "Código inválido ou expirado.");
    } finally {
      setLoading(false);
    }
  };

  const complete = digits.every(Boolean);

  return (
    <AuthLayout
      subtitle={
        <>
          Enviamos um código de {OTP_LENGTH} dígitos para{" "}
          <strong className={style.highlight}>{email}</strong>
        </>
      }
      footer={
        <>
          Não recebeu?{" "}
          <button type="button" className={style.linkButton} onClick={handleResend} disabled={resending}>
            {resending ? "Reenviando..." : "Reenviar código"}
          </button>
        </>
      }
    >
      <form onSubmit={handleSubmit} className={style.form}>
        <div className={style.otp} role="group" aria-label="Código de verificação">
          {digits.map((digit, i) => (
            <input
              key={i}
              ref={(el) => (inputsRef.current[i] = el)}
              type="text"
              inputMode="numeric"
              autoComplete={i === 0 ? "one-time-code" : "off"}
              aria-label={`Dígito ${i + 1}`}
              maxLength={1}
              value={digit}
              onChange={(e) => handleChange(i, e.target.value)}
              onKeyDown={(e) => handleKeyDown(i, e)}
              onPaste={handlePaste}
              autoFocus={i === 0}
            />
          ))}
        </div>

        <AuthError>{error}</AuthError>
        <AuthSubmit loading={loading} loadingLabel="Verificando..." disabled={!complete}>
          Verificar
        </AuthSubmit>
      </form>
    </AuthLayout>
  );
}
