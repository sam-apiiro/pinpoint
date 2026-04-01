import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { ConfigUsers, ErrorResponse, APP_PATH } from '@pinpoint-fe/constants';
import { usePostPublicUserRegistration } from '@pinpoint-fe/hooks';
import { ErrorToast } from '../components/Error/ErrorToast';
import { useReactToastifyToast } from '../components/Toast';
import { UserForm } from '../components/Config/users/UserForm';

export interface RegisterUserPageProps {}

export const RegisterUserPage = (_props: RegisterUserPageProps) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const toast = useReactToastifyToast();

  const { mutate: registerUser } = usePostPublicUserRegistration({
    onSuccess: () => {
      toast.success(t('CONFIGURATION.USERS.REGISTRATION.SUCCESS'));
      navigate(APP_PATH.SERVER_MAP);
    },
    onError: (error: ErrorResponse) => {
      toast.error(<ErrorToast error={error} />, {
        className: 'pointer-events-auto',
        bodyClassName: '!items-start',
        autoClose: false,
      });
    },
  });

  const handleSubmit = (user: ConfigUsers.User) => {
    registerUser(user);
  };

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <p className="text-sm text-muted-foreground">
        {t('CONFIGURATION.USERS.REGISTRATION.DESCRIPTION')}
      </p>
      <div className="rounded-lg border bg-card">
        <UserForm enableUserEdit={true} hideCancelButton={true} onSubmit={handleSubmit} />
      </div>
    </div>
  );
};
