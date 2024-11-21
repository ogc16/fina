import Link from 'next/link';
import tailwindConfig from '../../tailwind.config';

export default function Page() {
  const currentYear = new Date().getFullYear();

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-6">
      {/* <AcmeLogo /> */}
      <div className="text-center">
        <p className="text-3xl font-bold text-gray-800 md:text-5xl md:leading-normal animate-slide">
          Introducing <a className="text-green-500">Finify</a>
        </p>
        <p className="text-lg text-gray-700 md:text-xl">
          Your Ultimate app to manage your finances. Seamlessly send and manage your invoices.
        </p>

       <div className="flex gap-6 flex-nowrap mt-8">
          <button className="items-center gap-2 bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-400 md:text-base md:px-6 md:py-3"><Link href="/signing"className='rounded-xl'>
            Log in </Link>
          </button>
          <button className="flex items-center gap-2 bg-green-600 px-8 py-9 text-sm font-medium text-white transition-colors hover:bg-green-400 md:text-base md:px-6 md:py-3"><Link href="/signup"className='rounded-xl '>
            Create account</Link>
          </button>
        </div>
      </div>
      <footer className="text-center mt-10 pt-10 flex">
        <span>&copy; {currentYear} <i>Developed and maintained by</i> <strong><a href="https://">Fina.</a></strong></span>
      </footer>
    </main>
  );
}