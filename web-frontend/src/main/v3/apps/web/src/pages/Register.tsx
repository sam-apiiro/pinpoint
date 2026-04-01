import { FaUserPlus } from 'react-icons/fa';
import { useTranslation } from 'react-i18next';
import { MainHeader, RegisterUserPage } from '@pinpoint-fe/ui';
import { getLayoutWithSideNavigation } from '@/components/Layout/LayoutWithSideNavigation';

export interface RegisterProps {}

export const Register = (_props: RegisterProps) => {
  const { t } = useTranslation();

  return (
    <>
      <MainHeader
        title={
          <div className="flex items-center gap-2">
            <FaUserPlus />
            {t('CONFIGURATION.USERS.REGISTRATION.TITLE')}
          </div>
        }
      />
      <RegisterUserPage />
    </>
  );
};

export default () => getLayoutWithSideNavigation(<Register />);
